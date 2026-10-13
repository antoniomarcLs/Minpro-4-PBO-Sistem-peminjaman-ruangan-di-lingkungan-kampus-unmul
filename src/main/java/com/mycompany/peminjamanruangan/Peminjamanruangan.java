package com.mycompany.peminjamanruangan;

import com.mycompany.peminjamanruangan.controller.PeminjamanController;
import com.mycompany.peminjamanruangan.View.peminjamanView;
import java.util.Scanner;

public class Peminjamanruangan {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            peminjamanView view = new peminjamanView(scanner);
            PeminjamanController controller = new PeminjamanController(view);
            controller.jalankan();
        } catch (IllegalStateException e) {
            System.out.println(">> Aplikasi tidak dapat dijalankan: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println(">> Penyebab: " + e.getCause().getMessage());
            }
            System.out.println(">> Periksa apakah MariaDB berjalan dan db.properties sudah benar.");
        } finally {
            scanner.close();
        }
    }
}
