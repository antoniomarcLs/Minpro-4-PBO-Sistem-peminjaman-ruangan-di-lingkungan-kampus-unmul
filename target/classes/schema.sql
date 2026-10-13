CREATE DATABASE IF NOT EXISTS peminjaman_ruangan;
USE peminjaman_ruangan;

CREATE TABLE IF NOT EXISTS ruangan (
    id_ruangan         INT PRIMARY KEY,
    nama_ruangan       VARCHAR(100) NOT NULL,
    kapasitas          INT NOT NULL,
    tersedia           BOOLEAN NOT NULL DEFAULT TRUE,
    jenis_ruangan      VARCHAR(20) NOT NULL,
    fasilitas          VARCHAR(255),
    jenis_laboratorium VARCHAR(100),
    jumlah_kursi       INT,
    ada_proyektor      BOOLEAN
);

CREATE TABLE IF NOT EXISTS peminjam (
    id_peminjam   INT AUTO_INCREMENT PRIMARY KEY,
    nama          VARCHAR(100) NOT NULL,
    kategori      VARCHAR(20)  NOT NULL,
    nip           VARCHAR(50),
    nama_instansi VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS jadwal (
    id_jadwal   INT AUTO_INCREMENT PRIMARY KEY,
    tanggal     DATE NOT NULL,
    jam_mulai   TIME NOT NULL,
    jam_selesai TIME NOT NULL
);

CREATE TABLE IF NOT EXISTS peminjaman (
    nomor_surat INT PRIMARY KEY,
    id_peminjam INT NOT NULL,
    id_ruangan  INT NOT NULL,
    id_jadwal   INT NOT NULL,
    tujuan      VARCHAR(255) NOT NULL,
    FOREIGN KEY (id_peminjam) REFERENCES peminjam(id_peminjam),
    FOREIGN KEY (id_ruangan)  REFERENCES ruangan(id_ruangan),
    FOREIGN KEY (id_jadwal)   REFERENCES jadwal(id_jadwal)
);

-- Data master ruangan (INSERT IGNORE: aman dijalankan ulang)
INSERT IGNORE INTO ruangan
    (id_ruangan, nama_ruangan, kapasitas, tersedia, jenis_ruangan,
     fasilitas, jenis_laboratorium, jumlah_kursi, ada_proyektor)
VALUES
 (101, 'Kelas A1',       40, TRUE, 'Kelas Teori',  NULL, NULL, 40, TRUE),
 (102, 'Kelas A2',       35, TRUE, 'Kelas Teori',  NULL, NULL, 35, FALSE),
 (201, 'Meeting Room A', 15, TRUE, 'Meeting Room', 'Proyektor dan Smart TV', NULL, NULL, NULL),
 (301, 'Lab Komputer 1', 30, TRUE, 'Laboratorium', NULL, 'Komputer', NULL, NULL);
