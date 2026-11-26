<?php
declare(strict_types=1);
require_once __DIR__ . '/../middleware/CsrfMiddleware.php';
CsrfMiddleware::ensureSession();
$_SESSION = [];
session_destroy();
header('Location: login.php');
exit;
