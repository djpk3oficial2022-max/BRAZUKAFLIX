# BRAFLIX Android — publicação e atualização OTA assistida

Projeto Android de player M3U **somente de canais ao vivo**, sem listas, transmissões ou credenciais incluídas.

## O que realmente acontece
- **Canais**: altere a lista M3U na URL configurada; o app atualiza a lista na abertura e sob demanda, sem gerar APK.
- **Código/interface**: exige compilar um APK novo. O GitHub Actions pode fazê-lo na nuvem, sem Android Studio no computador, publicando-o no GitHub Releases.
- **Celular**: o BRAFLIX consulta `update.json` no GitHub Pages, avisa sobre novas versões, baixa o APK, compara o SHA-256, confere o pacote e a versão e solicita instalação pelo Android. **O Android exige autorização/confirmação.**
- A primeira instalação deve ser manual. Não há atualização silenciosa nem push em segundo plano.

## Configurar pela primeira vez (GitHub)
1. Crie uma conta GitHub e um **repositório público** próprio (ex.: `braflix-android`). Envie o **conteúdo da pasta BRAFLIX-Android** à raiz do repositório, incluindo `.github/workflows/publish.yml`. **Nunca** envie lista M3U privada, senhas, credenciais ou arquivo de assinatura ao GitHub.
2. Gere uma chave de assinatura **uma única vez** em um computador com Java JDK:
   `keytool -genkeypair -v -keystore braflix-release.jks -alias braflix -keyalg RSA -keysize 3072 -validity 10000`
   Guarde o `.jks` e as senhas fora do repositório, com backup. **Todas** as versões precisam dessa mesma chave.
3. Converta o JKS em texto base64 para criar um segredo no repositório. No PowerShell:
   `[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\caminho\braflix-release.jks'))`
4. Em Settings > Secrets and variables > Actions, crie estes segredos:
   - `BRAFLIX_KEYSTORE_BASE64`: valor base64 do JKS
   - `BRAFLIX_KEYSTORE_PASSWORD`: senha do arquivo
   - `BRAFLIX_KEY_ALIAS`: alias (no exemplo, `braflix`)
   - `BRAFLIX_KEY_PASSWORD`: senha da chave
5. Em Settings > Actions > General > Workflow permissions, permita `Read and write permissions` ao `GITHUB_TOKEN` para publicar release e branch `gh-pages`.
6. Em Actions > **Publicar BRAFLIX Android** > Run workflow, preencha a **primeira versão**: `version_name=1.0.0`, `version_code=1`, `release_notes=Primeira versão`.
7. Após o workflow publicar a branch `gh-pages`, vá a Settings > Pages e selecione **Deploy from a branch**, branch `gh-pages`, pasta `/ (root)`. Confira que `https://SEU_USUARIO.github.io/SEU_REPOSITORIO/update.json` é acessível.
8. Em Releases baixe `BRAFLIX-v1.0.0.apk` e instale manualmente no celular. A URL de atualização é configurada automaticamente na compilação do GitHub Actions.
9. Na próxima versão, altere o código no repositório e rode novamente o workflow com `version_name=1.1.0`, `version_code=2` e notas. Abra o BRAFLIX no celular: ao encontrar a versão nova ele apresenta o botão de atualização.

**IMPORTANTE:** não apague a chave de assinatura, não altere `applicationId=com.braflix.tv`, não diminua `versionCode`. Caso publique um APK assinado por outra chave, a atualização sobre a instalação existente falhará.

## Requisitos e verificações
- GitHub Pages/repositório público tornam **o APK e o manifesto acessíveis publicamente**. Não inclua conteúdo protegido, tokens ou listas privadas no APK ou no repositório.
- O workflow foi preparado, mas **não executado nesta sessão**. Valide o build e a instalação em um celular de teste.
- Github Releases pode redirecionar o download para outro host HTTPS; o downloader segue somente redirecionamentos HTTPS e confere o SHA-256 do manifesto. O Android confere a assinatura digital do APK.
- Se o manifesto for alterado por terceiros com acesso ao repositório, eles podem apontar para um APK novo; restrinja o acesso de escrita e use autenticação forte.
- Ações com a API de atualização podem depender das políticas de distribuição do Android; esta solução é destinada à distribuição própria fora da Play Store.
- Um build local via Android Studio também pode ser feito, mas deve usar a **mesma keystore** usada pelo GitHub Actions.

## Lista M3U
A lista de canais deve ser adicionada por arquivo ou URL no app, separadamente da publicação. Não incluímos a lista fornecida no pacote por conter links de acesso que podem ser privados. Use apenas streams devidamente autorizados.
