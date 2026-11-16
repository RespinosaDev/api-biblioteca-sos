#!/usr/bin/env bash
# Prueba de humo contra la API levantada con docker compose (la usa el CI).
# Recorre el flujo principal: usuario -> libro -> préstamo -> devolución.
set -euo pipefail
API=${API:-http://localhost:8080}

echo "Esperando a que la API esté lista..."
for i in $(seq 1 60); do
  if curl -fs "$API/actuator/health" | grep -q '"UP"'; then echo "API lista (${i}s)"; break; fi
  [ "$i" = 60 ] && { echo "La API no arrancó"; exit 1; }
  sleep 2
done

json() { curl -fs -H "Content-Type: application/json" "$@"; }

usuario=$(json -X POST "$API/api/usuarios" \
  -d '{"nombreUsuario":"ana_docker","matricula":"D001","fechaNacimiento":"2002-05-14","email":"ana@example.com"}' | jq -r .id)
libro=$(json -X POST "$API/api/libros" \
  -d '{"titulo":"Don Quijote de la Mancha","autor":"Miguel de Cervantes","isbn":"978-84-376-0494-7"}' | jq -r .id)
prestamo=$(curl -fs -X POST "$API/api/prestamos?usuarioId=$usuario&libroId=$libro" | jq -r .id)
echo "Usuario $usuario, libro $libro, préstamo $prestamo"

[ "$(curl -fs "$API/api/libros/$libro" | jq -r .disponible)" = "false" ] || { echo "El libro debería estar prestado"; exit 1; }
[ "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/api/prestamos?usuarioId=$usuario&libroId=$libro")" = "409" ] \
  || { echo "Prestar dos veces el mismo libro debería dar 409"; exit 1; }
curl -fs -X PUT "$API/api/prestamos/$prestamo/devolver" > /dev/null
[ "$(curl -fs "$API/api/libros/$libro" | jq -r .disponible)" = "true" ] || { echo "El libro debería estar disponible"; exit 1; }
[ "$(curl -s -o /dev/null -w '%{http_code}' "$API/api/usuarios/99999")" = "404" ] || { echo "Debería dar 404"; exit 1; }

echo "Prueba de humo superada: la API funciona en Docker con PostgreSQL"
