#### 1. Lembre de configurar suas credenciais no git para que possamos agradecer pela sua contribuição:

`git config --global user.name "YOUR NAME"`

`git config --global user.email "YOUR EMAIL ADDRESS"`

#### 2. O fluxo se inicia com o fork do [repositório](https://github.com/davyam/pgEasyReplication) para o seu próprio repositório público, isso pode ser feito diretamente pelo botão [[fork](https://github.com/davyam/pgEasyReplication#fork-destination-box)]
    
#### 3. Clone o seu fork público (remote/origin) para sua máquina de desenvolvimento (local)

`git clone https://github.com/[YOUR-USERNAME]/pgEasyReplication.git`

#### 4. Configure o repositório (upstream) nos remotos:
`git remote add upstream https://github.com/davyam/pgEasyReplication.git`

#### 5. Crie seu branch(feature ou hotfix) a partir do master
`git checkout -b [nova-funcionalidade] ou [hotfix-(numero-da-issue)]`

#### 6. Atualize seu branch com o upstream antes de enviar um pull request
```
git fetch upstream
git checkout master
git merge upstream/master
git checkout [branch-da-funcionalidade]
git rebase master
```

#### 7. Faça o pull request para o upstream/master
`git request-pull upstream/master [branch-da-funcionalidade]`
