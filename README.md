# Estoque +

Aplicativo Android para controle de produtos, estoque e movimentações.

## Prévia para portfólio

A edição `portfolio` é uma demonstração somente leitura, com produtos fictícios criados localmente na primeira abertura. Ela não altera o estoque e oferece contato por e-mail para solicitar informações sobre a versão completa.

Para gerar o APK no Windows:

```powershell
.\gradlew.bat :app:assemblePortfolioDebug
```

O arquivo gerado fica em `app/build/outputs/apk/portfolio/debug/app-portfolio-debug.apk`. Para disponibilizá-lo, publique esse APK em uma GitHub Release; os diretórios `build/` são saídas locais do Gradle.

## Limite da demonstração

O modo somente leitura é uma limitação da interface do APK de portfólio, não uma proteção de segurança. Como o código-fonte também está no repositório, os recursos da edição completa continuam visíveis no código. Não use essa variante para proteger dados ou regras de negócio.

## Contato

ams.macenasilva@gmail.com