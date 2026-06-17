{ pkgs, config, ... }:

let
  docsPort = 3000;
in
{
  languages.clojure.enable = true;
  languages.clojure.lsp.enable = true;

  packages = [
    pkgs.babashka
    pkgs.curl
    pkgs.git
    pkgs.nodejs
    pkgs.ripgrep
    pkgs.tmux
    pkgs.zsh
  ];

  env.RAMA_FACTORY_ENV = "development";
  env.RAMA_FACTORY_PORT = builtins.toString docsPort;

  tasks."rama:validate" = {
    exec = "clojure -M:factory validate";
  };

  tasks."rama:simulate" = {
    exec = "clojure -M:factory simulate demo-bank-transfer";
    after = [ "rama:validate" ];
  };

  tasks."swarm:config" = {
    exec = "clojure -M:factory swarm-config";
  };

  tasks."swarm:plan" = {
    exec = "clojure -M:factory swarm-plan";
  };

  tasks."mcp:serve" = {
    exec = "clojure -M:mcp";
  };

  tasks."assets:install" = {
    exec = "npm install";
  };

  tasks."assets:dev" = {
    exec = "npm install && npm run assets:dev";
  };

  tasks."assets:build" = {
    exec = "npm install && npm run assets:build";
  };

  tasks."rama:test" = {
    exec = "clojure -M:test";
  };

  tasks."docs:serve" = {
    exec = "clojure -M:docs ${builtins.toString docsPort}";
  };

  processes.docs = {
    exec = "clojure -M:docs ${builtins.toString docsPort}";
    ready.http.get = {
      port = docsPort;
      path = "/";
    };
    watch = {
      paths = [
        ./apps
        ./docs
        ./factory
        ./src
      ];
      extensions = [
        "clj"
        "edn"
        "md"
      ];
    };
  };

  enterShell = ''
    echo "Rama Factory"
    echo "  devenv up                         # run the dogfooded docs app"
    echo "  devenv test                       # run the verification suite"
    echo "  clojure -M:factory new <app>      # create a starter app"
    echo "  clojure -M:factory add auth --from factory/seeds/auth --target <app>"
    echo "  clojure -M:factory add factory-dashboard --from factory/seeds/factory-dashboard --target <app>"
    echo "  devenv tasks run rama:validate    # validate factory/challenge data"
    echo "  devenv tasks run rama:simulate    # generate the sample run"
    echo "  devenv tasks run swarm:plan       # inspect role worktrees"
    echo "  devenv tasks run swarm:config     # render SwarmForge window config"
    echo "  devenv tasks run assets:dev       # run Vite/Tailwind assets"
    echo "  devenv tasks run assets:build     # compile Vite/Tailwind assets"
    echo "  clojure -M:mcp                    # run the local stdio MCP server"
  '';

  enterTest = ''
    clojure -M:test
  '';
}
