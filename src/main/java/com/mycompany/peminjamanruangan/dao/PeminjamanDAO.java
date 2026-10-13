package com.mycompany.peminjamanruangan.dao;

import com.mycompany.peminjamanruangan.database.DatabaseConnection;
import com.mycompany.peminjamanruangan.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PeminjamanDAO {

    // ------------------------------------------------------------
    // SIMPAN: peminjam -> jadwal -> peminjaman (satu transaksi).
    // Ruangan TIDAK di-insert; harus sudah ada di tabel ruangan.
    // ------------------------------------------------------------
    public void simpan(Peminjaman p) throws SQLException {

        String sqlPeminjam = """
            INSERT INTO peminjam
                (nama, kategori, nip, nama_instansi)
            VALUES (?, ?, ?, ?)
            """;

        String sqlJadwal = """
            INSERT INTO jadwal
                (tanggal, jam_mulai, jam_selesai)
            VALUES (?, ?, ?)
            """;

        String sqlPeminjaman = """
            INSERT INTO peminjaman
                (nomor_surat, id_peminjam, id_ruangan,
                 id_jadwal, tujuan)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int idPeminjam;

                // 1. Simpan peminjam.
                try (PreparedStatement ps = conn.prepareStatement(
                        sqlPeminjam, Statement.RETURN_GENERATED_KEYS)) {

                    Peminjam peminjam = p.getPeminjam();

                    ps.setString(1, peminjam.getNama());
                    ps.setString(2, peminjam.getKategori());

                    switch (peminjam) {
                        case PeminjamInternal pi -> {
                            ps.setString(3, pi.getNip());
                            ps.setNull(4, Types.VARCHAR);
                        }
                        case PeminjamEksternal pe -> {
                            ps.setNull(3, Types.VARCHAR);
                            ps.setString(4, pe.getNamaInstansi());
                        }
                        default -> throw new SQLException(
                                "Kategori peminjam tidak dikenal.");
                    }

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("ID peminjam gagal dibuat.");
                        }
                        idPeminjam = keys.getInt(1);
                    }
                }

                // 2. Simpan jadwal.
                int idJadwal;

                try (PreparedStatement ps = conn.prepareStatement(
                        sqlJadwal, Statement.RETURN_GENERATED_KEYS)) {

                    Jadwal j = p.getJadwal();

                    ps.setDate(1, Date.valueOf(LocalDate.parse(j.getTanggal())));
                    ps.setTime(2, Time.valueOf(normalisasiJam(j.getJamMulai())));
                    ps.setTime(3, Time.valueOf(normalisasiJam(j.getJamSelesai())));

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("ID jadwal gagal dibuat.");
                        }
                        idJadwal = keys.getInt(1);
                    }
                }

                // 3. Simpan hubungan peminjaman (id_ruangan merujuk ruangan master).
                try (PreparedStatement ps = conn.prepareStatement(sqlPeminjaman)) {
                    ps.setInt(1, p.getNomorSurat());
                    ps.setInt(2, idPeminjam);
                    ps.setInt(3, p.getRuangan().getIdRuangan());
                    ps.setInt(4, idJadwal);
                    ps.setString(5, p.getTujuan());
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ------------------------------------------------------------
    // BACA SEMUA PEMINJAMAN
    // ------------------------------------------------------------
    public List<Peminjaman> findAll() throws SQLException {

        String sql = """
            SELECT
                p.nomor_surat, p.tujuan,
                pm.nama, pm.kategori, pm.nip, pm.nama_instansi,
                r.id_ruangan, r.nama_ruangan, r.kapasitas,
                r.tersedia, r.jenis_ruangan, r.fasilitas,
                r.jenis_laboratorium, r.jumlah_kursi,
                r.ada_proyektor,
                j.tanggal, j.jam_mulai, j.jam_selesai
            FROM peminjaman p
            JOIN peminjam pm ON p.id_peminjam = pm.id_peminjam
            JOIN ruangan r ON p.id_ruangan = r.id_ruangan
            JOIN jadwal j ON p.id_jadwal = j.id_jadwal
            ORDER BY p.nomor_surat
            """;

        List<Peminjaman> daftar = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String nama = rs.getString("nama");
                String kategori = rs.getString("kategori");

                Peminjam peminjam;
                if ("Internal".equals(kategori)) {
                    peminjam = new PeminjamInternal(nama, rs.getString("nip"));
                } else {
                    peminjam = new PeminjamEksternal(nama, rs.getString("nama_instansi"));
                }

                Ruangan ruangan = mapRuangan(rs);

                Jadwal jadwal = new Jadwal(
                        rs.getDate("tanggal").toString(),
                        rs.getTime("jam_mulai").toString().substring(0, 5),
                        rs.getTime("jam_selesai").toString().substring(0, 5));

                daftar.add(new Peminjaman(
                        peminjam,
                        rs.getString("tujuan"),
                        rs.getInt("nomor_surat"),
                        ruangan,
                        jadwal));
            }
        }

        return daftar;
    }

    // ------------------------------------------------------------
    // DAFTAR RUANGAN MASTER
    // ------------------------------------------------------------
    public List<Ruangan> findAllRuangan() throws SQLException {
        List<Ruangan> daftar = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM ruangan ORDER BY id_ruangan");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                daftar.add(mapRuangan(rs));
            }
        }
        return daftar;
    }

    // ------------------------------------------------------------
    // CEK BENTROK JADWAL
    // Dua rentang bentrok jika: mulaiLama < selesaiBaru DAN selesaiLama > mulaiBaru.
    // kecualiNomorSurat: abaikan surat ini (dipakai saat ganti ruangan).
    // Kirim -1 jika tidak ada yang dikecualikan.
    // ------------------------------------------------------------
    public boolean adaBentrok(int idRuangan, LocalDate tanggal,
                              String jamMulai, String jamSelesai,
                              int kecualiNomorSurat) throws SQLException {

        String sql = """
            SELECT 1
            FROM peminjaman p
            JOIN jadwal j ON p.id_jadwal = j.id_jadwal
            WHERE p.id_ruangan = ?
              AND j.tanggal = ?
              AND j.jam_mulai < ?
              AND j.jam_selesai > ?
              AND p.nomor_surat <> ?
            LIMIT 1
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idRuangan);
            ps.setDate(2, Date.valueOf(tanggal));
            ps.setTime(3, Time.valueOf(normalisasiJam(jamSelesai)));
            ps.setTime(4, Time.valueOf(normalisasiJam(jamMulai)));
            ps.setInt(5, kecualiNomorSurat);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ------------------------------------------------------------
    // HAPUS: peminjaman + peminjam + jadwal (satu transaksi).
    // Ruangan tidak dihapus karena data master.
    // ------------------------------------------------------------
    public boolean hapus(int nomorSurat) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int idPeminjam;
                int idJadwal;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id_peminjam, id_jadwal FROM peminjaman WHERE nomor_surat = ?")) {
                    ps.setInt(1, nomorSurat);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        idPeminjam = rs.getInt(1);
                        idJadwal = rs.getInt(2);
                    }
                }

                // Urutan penting: anak (peminjaman) dihapus sebelum induknya.
                hapusSatu(conn, "DELETE FROM peminjaman WHERE nomor_surat = ?", nomorSurat);
                hapusSatu(conn, "DELETE FROM peminjam WHERE id_peminjam = ?", idPeminjam);
                hapusSatu(conn, "DELETE FROM jadwal WHERE id_jadwal = ?", idJadwal);

                conn.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ------------------------------------------------------------
    // GANTI RUANGAN: ruangan baru harus sudah ada di tabel ruangan.
    // ------------------------------------------------------------
    public boolean gantiRuangan(int nomorSurat, int idRuanganBaru) throws SQLException {

        String sql = """
            UPDATE peminjaman
            SET id_ruangan = ?
            WHERE nomor_surat = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idRuanganBaru);
            ps.setInt(2, nomorSurat);

            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------
    // Helper privat
    // ------------------------------------------------------------
    private Ruangan mapRuangan(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_ruangan");
        String nama = rs.getString("nama_ruangan");
        int kapasitas = rs.getInt("kapasitas");
        boolean tersedia = rs.getBoolean("tersedia");
        String jenis = rs.getString("jenis_ruangan");

        return switch (jenis) {
            case "Meeting Room" -> new MeetingRoom(id, nama, kapasitas, tersedia,
                    rs.getString("fasilitas"));
            case "Laboratorium" -> new Laboratorium(id, nama, kapasitas, tersedia,
                    rs.getString("jenis_laboratorium"));
            case "Kelas Teori" -> new KelasTeori(id, nama, kapasitas, tersedia,
                    rs.getInt("jumlah_kursi"), rs.getBoolean("ada_proyektor"));
            default -> throw new SQLException("Jenis ruangan tidak dikenal: " + jenis);
        };
    }

    private void hapusSatu(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static String normalisasiJam(String jam) {
        return jam.length() == 5 ? jam + ":00" : jam;
    }
}
