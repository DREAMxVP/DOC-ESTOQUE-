# Estoque +

Aplicativo Android para controle de produtos, estoque e movimentações.

## Prévia para portfólio

A prévia web estará em [dreamxvp.github.io/DOC-ESTOQUE-](https://dreamxvp.github.io/DOC-ESTOQUE-/) após a ativação inicial em **Settings > Pages > Build and deployment > Source: GitHub Actions**. Depois disso, o workflow publica o conteúdo de `docs/` automaticamente a cada atualização.

Para gerar o APK no Windows:

```powershell
.\gradlew.bat :app:assemblePortfolioDebug
```

O arquivo gerado fica em `app/build/outputs/apk/portfolio/debug/app-portfolio-debug.apk`. Para atualizar o download do site, copie o APK gerado para `docs/estoque-mais-portfolio.apk` antes de enviar as alterações. No Android, pode ser necessário autorizar a instalação de aplicativos baixados.

## Limite da demonstração

O modo somente leitura é uma limitação da interface do APK de portfólio, não uma proteção de segurança. Como o código-fonte também está no repositório, os recursos da edição completa continuam visíveis no código. Não use essa variante para proteger dados ou regras de negócio.

## Contato

ams.macenasilva@gmail.com