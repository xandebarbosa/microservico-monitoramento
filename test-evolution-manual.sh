#!/bin/bash

echo "🔍 Testando conexão manual com Evolution API..."

echo "1. Testando endpoint /instance/fetchInstances:"
docker-compose exec evolution-api curl -s "http://localhost:8080/instance/fetchInstances?apikey=${EVOLUTION_API_TOKEN}"

echo ""
echo "2. Testando endpoint /instance/connectionState/RadarBot:"
docker-compose exec evolution-api curl -s "http://localhost:8080/instance/connectionState/RadarBot?apikey=${EVOLUTION_API_TOKEN}"

echo ""
echo "3. Testando microserviço /api/evolution/status:"
docker-compose exec microservico-monitoramento curl -s http://localhost:8089/api/evolution/status

echo ""
echo "4. Verificando instância criada no Evolution Manager:"
echo "Acesse: http://localhost:8091"
echo "Token: ${EVOLUTION_API_TOKEN}"