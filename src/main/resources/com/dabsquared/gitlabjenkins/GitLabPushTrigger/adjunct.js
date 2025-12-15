Behaviour.specify("BUTTON.gitlab_plugin-generate", "gitlab_plugin-generate", 0, function (e) {
    e.onclick = function (evt) {
        const array = new Uint32Array(4);
        self.crypto.getRandomValues(array);
        document.getElementById('gitlab_plugin_secretToken').value = Array.from(array).map(e => e.toString(16)).join("");
        evt.preventDefault();
    };
});

Behaviour.specify("BUTTON.gitlab_plugin-clear", "gitlab_plugin-clear", 0, function (e) {
    e.onclick = function (evt) {
        document.getElementById('gitlab_plugin_secretToken').value = "";
        evt.preventDefault();
    };
});
