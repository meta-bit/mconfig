mermaid.initialize({
    startOnLoad: true,
    securityLevel: 'loose',
    theme: 'default'
});

// Force re-scan of the specific code blocks created by fenced_code
document.addEventListener("DOMContentLoaded", function() {
    const blocks = document.querySelectorAll(".language-mermaid");
    blocks.forEach((block, i) => {
        const container = document.createElement("div");
        container.id = "mermaid-graph-" + i;
        container.className = "mermaid";
        container.textContent = block.textContent;
        block.parentElement.replaceWith(container);
    });
    mermaid.init(undefined, ".mermaid");
});