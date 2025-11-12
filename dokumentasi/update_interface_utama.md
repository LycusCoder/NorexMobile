### 🚀 Ide Tweak Lanjut: Membuatnya Hyper-Fokus

1.  **Fokuskan FAB Jual (Transisi ke Transaksi)**

    Saat ini, FAB di tengah bawah sudah menjadi ikon Transaksi. Supaya alurnya makin seamless:

    *   **Pindahkan Logic**: Pastikan klik FAB ini langsung memicu fungsi yang ada di `TransaksiFragment` (yaitu membuka scanner atau search produk), bukan sekadar navigasi ke fragment. Tujuan utamanya: mulai menjual.
    *   **Visual FAB**: Ganti ikon `ic_transaksi_baru` di FAB menjadi ikon "Scan" (`ic_scan`) jika memang default action kasir adalah scan. Atau biarkan ikon tetap "nota/transaksi" jika default adalah search.

2.  **Optimalkan Kartu Statistik (Relevansi UMKM)**

    Tiga kartu statistikmu (Produk, Transaksi, Pendapatan) sudah bagus. Sekarang kita bikin lebih fokus pada kinerja harian.

    *   **Ganti "Produk"**: Data "Total Produk" (0 Produk) sifatnya statis. Ganti ini dengan metrik yang lebih dinamis dan daily-focused, misalnya:
        *   **Keuntungan Harian (Profit)**. (Jika kamu sudah implement fitur margin seperti di `FUTURE_FEATURES.md`).
        *   **Rata-rata Nilai Transaksi (AVT)**: Insight bagus untuk tahu seberapa banyak pelanggan berbelanja.

3.  **Perbaiki Area "Transaksi Terakhir" (Minimalis Tapi Informatif)**

    Bagian ini harus memberikan glanceable history tanpa bikin pusing.

    *   **Tambahkan Aksi "Lihat Semua"**: Di samping judul "Transaksi Terakhir", tambahkan teks atau ikon kecil (`ic_chevron_right`) yang mengarahkan ke halaman Riwayat Transaksi (Laporan).

4.  **Ikonografi yang Polished**

    Ikon di Quick Actions masih terlihat simple. Kita bisa buat mereka lebih pop dengan:

    *   **Background Warna/Shape Khusus**: Beri background lingkaran (misalnya pakai `ic_circle` atau `circle_preview`) dengan warna aksen yang berbeda (misal: Hijau untuk Add, Biru untuk Scan) supaya lebih menarik dan gampang diidentifikasi daripada hanya kartu putih polos.
