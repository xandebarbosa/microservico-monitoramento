#!/bin/bash

echo "🔍 Testando conectividade de rede entre containers..."

echo "1. Verificando se o container evolution-api está acessível:"
docker-compose exec microservico-monitoramento ping -c 3 evolution-api

echo ""
echo "2. Testando resolução DNS dentro do container:"
docker-compose exec microservico-monitoramento nslookup evolution-api

echo ""
echo "3. Testando conexão na porta 8080:"
docker-compose exec microservico-monitoramento nc -zv evolution-api 8080 && echo "✅ Porta 8080 acessível" || echo "❌ Porta 8080 inacessível"

echo ""
echo "4. Testando requisição HTTP direta:"
docker-compose exec microservico-monitoramento curl -v "http://evolution-api:8080/instance/fetchInstances?apikey=${EVOLUTION_API_TOKEN}" 2>&1 | grep -E "(HTTP|< HTTP|CONNECT|Failed)"

echo ""
echo "5. Verificando variáveis de ambiente:"
docker-compose exec microservico-monitoramento env | grep EVOLUTION

echo ""
echo "6. Testando se o Evolution API está respondendo de fora:"
curl -s -o /dev/null -w "HTTP Code: %{http_code}\n" "http://localhost:8090/instance/fetchInstances?apikey=${EVOLUTION_API_TOKEN}"