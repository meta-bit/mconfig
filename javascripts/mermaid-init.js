mermaid.initialize({
    startOnLoad: true,
    queryMethods: [".language-mermaid"],
    theme: "default"
});

// Manual trigger for MkDocs/Material compatibility
document.addEventListener("DOMContentLoaded", function() {
    mermaid.init(undefined, ".language-mermaid");
});