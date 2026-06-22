<#
.SYNOPSIS
    Dispara las transacciones de la homologacion de tarjetas de Fiserv contra el backend.

.DESCRIPTION
    Para cada COMPRA pega contra POST /api/admin/payments/homologation, se guarda
    reference + token + pageUrl en un CSV y genera un HTML auto-submit (form POST con el
    token) para que abras y pagues en la pagina de Fiserv con la tarjeta de prueba.
    Las ANULACIONES llaman a POST /api/admin/payments/{reference}/void usando el reference
    de la compra previa (que ya tiene que estar pagada).

    Las cuotas y el CVV se eligen en la pagina de Fiserv, no los manda el backend; el script
    te los recuerda por fila.

.EXAMPLE
    .\homologacion.ps1 -Row 1            # dispara la fila 1 y abre el HTML para pagar
    .\homologacion.ps1 -Row 18           # fila 18 (Ley 19210, indi=6)
    .\homologacion.ps1 -VoidRow 3        # anula la compra de la fila 2 (anulacion #2)
    .\homologacion.ps1 -ListRows         # lista todas las filas y su estado
#>
[CmdletBinding(DefaultParameterSetName = 'Fire')]
param(
    [Parameter(ParameterSetName = 'Fire')]
    [int]$Row,

    [Parameter(ParameterSetName = 'Void')]
    [int]$VoidRow,

    [Parameter(ParameterSetName = 'List')]
    [switch]$ListRows,

    [string]$BaseUrl = 'https://lauramansilla.com',
    [string]$OutDir = (Join-Path $PSScriptRoot 'homologacion-out'),
    [switch]$NoOpen   # no abrir el HTML automaticamente
)

$ErrorActionPreference = 'Stop'

# Todas las transacciones de la planilla. Tipo COMPRA = se dispara; ANULACION = void de
# la fila destino (VoidTarget). indi: 0 normal, 6 Ley 19210. Cuotas/CVV van en la pagina.
$Transacciones = @(
    [pscustomobject]@{ Row=1;  Tipo='COMPRA';    Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='UYU'; Monto=79.99; Indi=0; Cuotas=1; CVV='No';  Esperado='Autorizada' }
    [pscustomobject]@{ Row=2;  Tipo='COMPRA';    Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='UYU'; Monto=80;    Indi=0; Cuotas=2; CVV='No';  Esperado='Autorizada' }
    [pscustomobject]@{ Row=3;  Tipo='ANULACION'; Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='UYU'; Monto=80;    Indi=0; Cuotas=$null; CVV=$null; Esperado='Autorizada'; VoidTarget=2 }
    [pscustomobject]@{ Row=4;  Tipo='COMPRA';    Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='UYU'; Monto=51.51; Indi=0; Cuotas=1; CVV='No';  Esperado='Rechazada' }
    [pscustomobject]@{ Row=5;  Tipo='COMPRA';    Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='USD'; Monto=50.01; Indi=0; Cuotas=5; CVV='No';  Esperado='Autorizada' }
    [pscustomobject]@{ Row=6;  Tipo='COMPRA';    Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='USD'; Monto=60;    Indi=0; Cuotas=1; CVV='No';  Esperado='Autorizada' }
    [pscustomobject]@{ Row=7;  Tipo='ANULACION'; Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='USD'; Monto=60;    Indi=0; Cuotas=$null; CVV=$null; Esperado='Autorizada'; VoidTarget=6 }
    [pscustomobject]@{ Row=8;  Tipo='COMPRA';    Cliente='CLIENTE_2'; Tarjeta='VISA';       Moneda='UYU'; Monto=122;   Indi=0; Cuotas=1; CVV='123'; Esperado='Autorizada' }
    [pscustomobject]@{ Row=9;  Tipo='ANULACION'; Cliente='CLIENTE_2'; Tarjeta='VISA';       Moneda='UYU'; Monto=122;   Indi=0; Cuotas=$null; CVV=$null; Esperado='Autorizada'; VoidTarget=8 }
    [pscustomobject]@{ Row=10; Tipo='COMPRA';    Cliente='CLIENTE_3'; Tarjeta='ANDA';       Moneda='UYU'; Monto=7;     Indi=0; Cuotas=1; CVV='No';  Esperado='Autorizada' }
    [pscustomobject]@{ Row=11; Tipo='ANULACION'; Cliente='CLIENTE_3'; Tarjeta='ANDA';       Moneda='UYU'; Monto=7;     Indi=0; Cuotas=$null; CVV=$null; Esperado='Autorizada'; VoidTarget=10 }
    [pscustomobject]@{ Row=12; Tipo='COMPRA';    Cliente='CLIENTE_3'; Tarjeta='ANDA';       Moneda='UYU'; Monto=1100;  Indi=0; Cuotas=2; CVV='No';  Esperado='Autorizada' }
    [pscustomobject]@{ Row=13; Tipo='COMPRA';    Cliente='CLIENTE_3'; Tarjeta='ANDA';       Moneda='USD'; Monto=550;   Indi=0; Cuotas=2; CVV='No';  Esperado='Autorizada' }
    [pscustomobject]@{ Row=14; Tipo='COMPRA';    Cliente='CLIENTE_4'; Tarjeta='MASTERCARD'; Moneda='UYU'; Monto=77;    Indi=0; Cuotas=1; CVV='123'; Esperado='Autorizada' }
    [pscustomobject]@{ Row=15; Tipo='COMPRA';    Cliente='CLIENTE_4'; Tarjeta='MASTERCARD'; Moneda='USD'; Monto=150;   Indi=0; Cuotas=1; CVV='123'; Esperado='Autorizada' }
    [pscustomobject]@{ Row=16; Tipo='COMPRA';    Cliente='CLIENTE_4'; Tarjeta='MASTERCARD'; Moneda='UYU'; Monto=2500;  Indi=0; Cuotas=2; CVV='123'; Esperado='Autorizada' }
    [pscustomobject]@{ Row=17; Tipo='COMPRA';    Cliente='CLIENTE_4'; Tarjeta='MASTERCARD'; Moneda='UYU'; Monto=150;   Indi=0; Cuotas=1; CVV='111'; Esperado='Autorizada' }
    [pscustomobject]@{ Row=18; Tipo='COMPRA';    Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='UYU'; Monto=244;   Indi=6; Cuotas=1; CVV='No';  Esperado='Autorizada'; Ley='Ley 19210' }
    [pscustomobject]@{ Row=19; Tipo='COMPRA';    Cliente='CLIENTE_1'; Tarjeta='CREDDIR';    Moneda='USD'; Monto=122;   Indi=6; Cuotas=1; CVV='No';  Esperado='Autorizada'; Ley='Ley 19210' }
)

# Numeros de tarjeta para el recordatorio al pagar.
$Tarjetas = @{
    'CREDDIR'    = '6018*2811*1160*1829  (venc 10/2029, CVV 1234)'
    'ANDA'       = '6031*9912*4820*0370  (venc 03/2029, CVV 1974)'
    'MASTERCARD' = '5101*9800*0000*0000  (venc 12/2029, CVV 123)'
    'VISA'       = '4103*7700*0000*0006  (venc 12/2029, CVV 123)'
}

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }
$CsvPath = Join-Path $OutDir 'resultados.csv'

function Get-Resultados {
    if (Test-Path $CsvPath) { @(Import-Csv $CsvPath) } else { @() }
}

function Save-Resultado($obj) {
    $rows = Get-Resultados | Where-Object { [int]$_.Row -ne [int]$obj.Row }
    $rows = @($rows) + $obj
    $rows | Sort-Object { [int]$_.Row } | Export-Csv -Path $CsvPath -NoTypeInformation -Encoding UTF8
}

function New-PagoHtml($tx, $reference, $token, $pageUrl) {
    $cardInfo = $Tarjetas[$tx.Tarjeta]
    $htmlPath = Join-Path $OutDir ("pago-fila-{0}.html" -f $tx.Row)
    $html = @"
<!DOCTYPE html>
<html lang="es">
<head><meta charset="utf-8"><title>Homologacion fila $($tx.Row)</title></head>
<body onload="document.forms[0].submit()">
  <h3>Fila $($tx.Row) - $($tx.Tipo) - $($tx.Moneda) $($tx.Monto)</h3>
  <p><b>Tarjeta:</b> $($tx.Tarjeta) $cardInfo</p>
  <p><b>Cuotas a elegir:</b> $($tx.Cuotas) &nbsp; | &nbsp; <b>Ingresar CVV:</b> $($tx.CVV)</p>
  <p><b>Esperado:</b> $($tx.Esperado)</p>
  <p>Redirigiendo a Fiserv... si no salta solo, apreta el boton.</p>
  <form method="POST" action="$pageUrl">
    <input type="hidden" name="token" value="$token">
    <button type="submit">Ir a pagar</button>
  </form>
</body>
</html>
"@
    Set-Content -Path $htmlPath -Value $html -Encoding UTF8
    return $htmlPath
}

# ---- Modo LISTA ----
if ($ListRows) {
    $done = Get-Resultados
    $Transacciones | ForEach-Object {
        $tx = $_
        $r = $done | Where-Object { [int]$_.Row -eq $tx.Row }
        $estado = if ($r) { if ($r.Resultado) { $r.Resultado } else { 'disparada, falta pagar/registrar' } } else { '(pendiente)' }
        [pscustomobject]@{
            Fila=$tx.Row; Tipo=$tx.Tipo; Cliente=$tx.Cliente; Tarjeta=$tx.Tarjeta
            Moneda=$tx.Moneda; Monto=$tx.Monto; Indi=$tx.Indi; Cuotas=$tx.Cuotas
            Esperado=$tx.Esperado; Estado=$estado
        }
    } | Format-Table -AutoSize
    return
}

# ---- Modo VOID (anulacion) ----
if ($PSCmdlet.ParameterSetName -eq 'Void') {
    $tx = $Transacciones | Where-Object { $_.Row -eq $VoidRow }
    if (-not $tx) { throw "No existe la fila $VoidRow" }
    if ($tx.Tipo -ne 'ANULACION') { throw "La fila $VoidRow no es una ANULACION" }

    $target = Get-Resultados | Where-Object { [int]$_.Row -eq $tx.VoidTarget }
    if (-not $target) { throw "Primero corre y paga la fila $($tx.VoidTarget) (la compra a anular)" }
    $reference = $target.Reference

    Write-Host "Anulando fila $($tx.VoidTarget) (reference $reference)..." -ForegroundColor Yellow
    $status = Invoke-RestMethod -Uri "$BaseUrl/api/admin/payments/$reference/void" -Method Post
    Write-Host "Resultado void: $status" -ForegroundColor Green

    Save-Resultado([pscustomobject]@{
        Row=$tx.Row; Tipo=$tx.Tipo; Cliente=$tx.Cliente; Tarjeta=$tx.Tarjeta
        Moneda=$tx.Moneda; Monto=$tx.Monto; Indi=$tx.Indi; Cuotas=''
        Esperado=$tx.Esperado; Reference=$reference; Token=$target.Token
        PageUrl=''; FiredAt=(Get-Date -Format 's'); CodRespuesta=''; Resultado="VOID:$status"
    })
    return
}

# ---- Modo FIRE (compra) ----
if (-not $Row) { throw "Indica una fila: -Row N  (o -VoidRow N para anular, -ListRows para ver)" }

$tx = $Transacciones | Where-Object { $_.Row -eq $Row }
if (-not $tx) { throw "No existe la fila $Row" }
if ($tx.Tipo -eq 'ANULACION') { throw "La fila $Row es una ANULACION: usa -VoidRow $Row" }

$body = @{ amount = $tx.Monto; currency = $tx.Moneda; indi = $tx.Indi } | ConvertTo-Json

Write-Host "Disparando fila $Row : $($tx.Moneda) $($tx.Monto) indi=$($tx.Indi) ..." -ForegroundColor Cyan
$resp = Invoke-RestMethod -Uri "$BaseUrl/api/admin/payments/homologation" -Method Post -ContentType 'application/json' -Body $body

Write-Host "  reference : $($resp.reference)"
Write-Host "  token     : $($resp.token)"
Write-Host "  pageUrl   : $($resp.pageUrl)"

$htmlPath = New-PagoHtml $tx $resp.reference $resp.token $resp.pageUrl

Save-Resultado([pscustomobject]@{
    Row=$tx.Row; Tipo=$tx.Tipo; Cliente=$tx.Cliente; Tarjeta=$tx.Tarjeta
    Moneda=$tx.Moneda; Monto=$tx.Monto; Indi=$tx.Indi; Cuotas=$tx.Cuotas
    Esperado=$tx.Esperado; Reference=$resp.reference; Token=$resp.token
    PageUrl=$resp.pageUrl; FiredAt=(Get-Date -Format 's'); CodRespuesta=''; Resultado=''
})

Write-Host ""
Write-Host "Pagar con: $($tx.Tarjeta) $($Tarjetas[$tx.Tarjeta])" -ForegroundColor Magenta
Write-Host "Cuotas: $($tx.Cuotas)  |  Ingresar CVV: $($tx.CVV)  |  Esperado: $($tx.Esperado)" -ForegroundColor Magenta
Write-Host "HTML de pago: $htmlPath"

if (-not $NoOpen) { Start-Process $htmlPath }
