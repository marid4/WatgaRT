<?php
declare(strict_types=1);

/**
 * Pure-PHP PDF 1.4 Generator (Zero Composer dependencies - cPanel Ready)
 * Menghasilkan dokumen PDF resmi untuk Surat Pengantar, Bukti Pembayaran, dan Laporan Kas.
 */
final class PdfExportService
{
    public static function generateSimplePdf(string $title, array $lines, string $footer = 'Dokumen resmi diterbitkan oleh sistem wargaRT'): string
    {
        $esc = static fn(string $s): string => str_replace(['\\', '(', ')'], ['\\\\', '\\(', '\\)'], $s);

        $stream = "BT\n";
        $stream .= "/F2 15 Tf\n50 780 Td\n(" . $esc($title) . ") Tj\n";
        $stream .= "/F1 9 Tf\n0 -18 Td\n(--------------------------------------------------------------------------------------) Tj\n";
        $stream .= "/F1 10.5 Tf\n0 -20 Td\n";

        foreach ($lines as $line) {
            $wrapped = wordwrap((string) $line, 86, "\n", true);
            foreach (explode("\n", $wrapped) as $subLine) {
                $stream .= '(' . $esc($subLine) . ") Tj\n0 -16 Td\n";
            }
        }

        $stream .= "/F1 8.5 Tf\n0 -24 Td\n(" . $esc($footer) . ") Tj\nET\n";

        $objects = [];
        $objects[] = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
        $objects[] = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n";
        $objects[] = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n";
        $objects[] = "4 0 obj\n<< /Length " . strlen($stream) . " >>\nstream\n" . $stream . "endstream\nendobj\n";
        $objects[] = "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";
        $objects[] = "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n";

        $pdf = "%PDF-1.4\n";
        $offsets = [0];
        foreach ($objects as $obj) {
            $offsets[] = strlen($pdf);
            $pdf .= $obj;
        }
        $xrefOffset = strlen($pdf);
        $pdf .= "xref\n0 " . (count($objects) + 1) . "\n0000000000 65535 f \n";
        for ($i = 1; $i <= count($objects); $i++) {
            $pdf .= sprintf("%010d 00000 n \n", $offsets[$i]);
        }
        $pdf .= "trailer\n<< /Size " . (count($objects) + 1) . " /Root 1 0 R >>\nstartxref\n{$xrefOffset}\n%%EOF";
        return $pdf;
    }
}
