<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/config.php';

final class EncryptionService
{
    private const CIPHER = 'aes-256-gcm';

    private static function getKey(): string
    {
        $raw = base64_decode(DATA_ENCRYPTION_KEY, true);
        if ($raw === false || strlen($raw) < 32) {
            return hash('sha256', DATA_ENCRYPTION_KEY, true);
        }
        return substr($raw, 0, 32);
    }

    public static function encrypt(string $plaintext): string
    {
        $iv = random_bytes(12);
        $tag = '';
        $ciphertext = openssl_encrypt(
            $plaintext,
            self::CIPHER,
            self::getKey(),
            OPENSSL_RAW_DATA,
            $iv,
            $tag,
            '',
            16
        );
        if ($ciphertext === false) {
            throw new RuntimeException('Gagal mengenkripsi data sensitif.');
        }
        return base64_encode($iv . $tag . $ciphertext);
    }

    public static function decrypt(string $encoded): string
    {
        if (str_starts_with($encoded, 'DEMO_ENC_')) {
            return substr($encoded, 9);
        }
        $raw = base64_decode($encoded, true);
        if ($raw === false || strlen($raw) <= 28) {
            return '';
        }
        $iv = substr($raw, 0, 12);
        $tag = substr($raw, 12, 16);
        $ciphertext = substr($raw, 28);
        $plain = openssl_decrypt($ciphertext, self::CIPHER, self::getKey(), OPENSSL_RAW_DATA, $iv, $tag);
        return $plain === false ? '' : $plain;
    }

    public static function blindHash(string $value): string
    {
        return hash('sha256', trim($value));
    }

    public static function maskIdNumber(string $value): string
    {
        $clean = preg_replace('/\D+/', '', $value) ?? $value;
        $len = strlen($clean);
        if ($len <= 8) {
            return str_repeat('*', $len);
        }
        return substr($clean, 0, 6) . str_repeat('*', $len - 10) . substr($clean, -4);
    }
}
