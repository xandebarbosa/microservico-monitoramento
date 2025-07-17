# =====================================================================
# ESTÁGIO 1: Build da Aplicação com Maven
# Usa uma imagem oficial do Maven com Java 21 (ou a versão que você usa).
# Apelidamos este estágio de 'build'.
# =====================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia primeiro o pom.xml para aproveitar o cache de camadas do Docker
COPY pom.xml .
#RUN mvn dependency:go-offline

# Copia o resto do código-fonte da sua aplicação
COPY src ./src

# Executa o comando do Maven para compilar e empacotar a aplicação em um .jar, pulando os testes.
RUN mvn clean package -DskipTests


# =====================================================================
# ESTÁGIO 2: Criação da Imagem Final de Execução
# Usa uma imagem Java muito menor, apenas com o ambiente de execução (JRE).
# =====================================================================
FROM eclipse-temurin:21-jdk
VOLUME /tmp
# Define o diretório de trabalho
WORKDIR /app

# Copia o arquivo .jar gerado no Estágio 1 para a imagem final
# e o renomeia para app.jar para facilitar a execução.
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta em que a aplicação Spring Boot irá rodar.
# IMPORTANTE: Este número deve ser o mesmo do 'server.port' no seu application.properties.
# Se seu serviço roda na 8085, mantenha 8085. Se for 8082, mude para 8082, etc.
EXPOSE 8089

# Comando que será executado quando o contêiner iniciar.
# Ele simplesmente executa o arquivo .jar da sua aplicação.
ENTRYPOINT ["java", "-jar", "app.jar"]