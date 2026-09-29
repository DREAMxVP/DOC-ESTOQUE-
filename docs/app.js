const products = [
    {
        id: "cafe", name: "Café especial 500 g", description: "Torra média, grãos selecionados",
        category: "Mercearia", location: "Prateleira A1", quantity: 18, minimum: 5,
        cost: 12.5, price: 24.9, movements: 24,
        image: "https://images.unsplash.com/photo-1442512595331-e89e73853f31?auto=format&fit=crop&w=180&h=180&q=78"
    },
    {
        id: "caderno", name: "Caderno pontilhado A5", description: "Capa dura, 160 páginas",
        category: "Papelaria", location: "Prateleira B2", quantity: 7, minimum: 8,
        cost: 18, price: 34.9, movements: 17,
        image: "https://images.unsplash.com/photo-1517842645767-c639042777db?auto=format&fit=crop&w=180&h=180&q=78"
    },
    {
        id: "caneca", name: "Caneca térmica 450 ml", description: "Aço inoxidável, azul",
        category: "Acessórios", location: "Prateleira C1", quantity: 0, minimum: 3,
        cost: 29, price: 59.9, movements: 13,
        image: "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=180&h=180&q=78"
    },
    {
        id: "marcadores", name: "Kit de marcadores", description: "Conjunto com 12 cores",
        category: "Papelaria", location: "Prateleira B1", quantity: 12, minimum: 4,
        cost: 15, price: 32.5, movements: 10,
        image: "https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=180&h=180&q=78"
    },
    {
        id: "agenda", name: "Agenda semanal", description: "Capa verde, edição demonstrativa",
        category: "Papelaria", location: "Prateleira A2", quantity: 3, minimum: 5,
        cost: 22, price: 45, movements: 8,
        image: "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&w=180&h=180&q=78"
    }
];

const movements = [
    { productId: "cafe", type: "in", quantity: 12, time: "Hoje, 09:42" },
    { productId: "caderno", type: "out", quantity: 3, time: "Hoje, 08:18" },
    { productId: "caneca", type: "out", quantity: 2, time: "Ontem, 16:07" },
    { productId: "agenda", type: "out", quantity: 1, time: "Ontem, 14:30" }
];

const currency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const productList = document.querySelector("#product-list");
const searchInput = document.querySelector("#product-search");
const dialog = document.querySelector("#product-dialog");
const dialogContent = document.querySelector("#dialog-content");
let activeFilter = "all";

function getStockState(product) {
    if (product.quantity === 0) return { label: "Sem estoque", className: "is-empty", filter: "empty" };
    if (product.quantity <= product.minimum) return { label: "Estoque baixo", className: "is-low", filter: "low" };
    return { label: "Disponível", className: "", filter: "available" };
}

function createProductRow(product, index) {
    const state = getStockState(product);
    const row = document.createElement("article");
    row.className = "product-row";
    row.style.animationDelay = `${Math.min(index, 5) * 35}ms`;

    const main = document.createElement("div");
    main.className = "product-main";
    const thumb = document.createElement("div");
    thumb.className = "product-thumb";
    const image = document.createElement("img");
    image.src = product.image;
    image.alt = "";
    image.loading = "lazy";
    image.addEventListener("error", () => {
        const fallback = document.createElement("span");
        fallback.className = "image-fallback";
        fallback.setAttribute("aria-hidden", "true");
        fallback.textContent = product.name.charAt(0);
        thumb.replaceChildren(fallback);
    }, { once: true });
    thumb.append(image);

    const copy = document.createElement("div");
    copy.className = "product-copy";
    const name = document.createElement("p");
    name.className = "product-name";
    name.textContent = product.name;
    const description = document.createElement("p");
    description.className = "product-description";
    description.textContent = product.description;
    copy.append(name, description);
    main.append(thumb, copy);

    const category = document.createElement("span");
    category.className = "product-category";
    category.textContent = product.category;
    const quantity = document.createElement("span");
    quantity.className = `product-quantity stock-status ${state.className}`;
    quantity.textContent = `${product.quantity} un. · ${state.label}`;
    const price = document.createElement("span");
    price.className = "product-price";
    price.textContent = currency.format(product.price);
    const details = document.createElement("button");
    details.className = "row-action";
    details.type = "button";
    details.textContent = "Detalhes";
    details.setAttribute("aria-label", `Ver detalhes de ${product.name}`);
    details.addEventListener("click", () => showProductDetails(product));

    row.append(main, category, quantity, price, details);
    return row;
}

function renderProducts() {
    const query = searchInput.value.trim().toLocaleLowerCase("pt-BR");
    const filtered = products.filter((product) => {
        const searchable = `${product.name} ${product.description} ${product.category}`.toLocaleLowerCase("pt-BR");
        const matchesSearch = searchable.includes(query);
        const matchesFilter = activeFilter === "all" || getStockState(product).filter === activeFilter;
        return matchesSearch && matchesFilter;
    });

    productList.replaceChildren(...filtered.map(createProductRow));
    document.querySelector("#product-count").textContent = `${filtered.length} ${filtered.length === 1 ? "item" : "itens"}`;
    document.querySelector("#empty-results").hidden = filtered.length !== 0;
}

function updateSummary() {
    const units = products.reduce((total, product) => total + product.quantity, 0);
    const totalCost = products.reduce((total, product) => total + product.cost * product.quantity, 0);
    const alerts = products.filter((product) => product.quantity <= product.minimum).length;

    document.querySelector("#metric-products").textContent = products.length;
    document.querySelector("#metric-units").textContent = units;
    document.querySelector("#metric-value").textContent = currency.format(totalCost);
    document.querySelector("#metric-alerts").textContent = alerts;

    for (const filter of ["all", "low", "empty"]) {
        const count = filter === "all"
            ? products.length
            : products.filter((product) => getStockState(product).filter === filter).length;
        document.querySelector(`[data-count="${filter}"]`).textContent = count;
    }
}

function renderReports() {
    document.querySelector("#report-movements").textContent = movements.length;
    const movementList = document.querySelector("#movement-list");
    movementList.replaceChildren(...movements.map((movement) => {
        const product = products.find((item) => item.id === movement.productId);
        const item = document.createElement("li");
        item.className = "movement-item";
        const copy = document.createElement("span");
        copy.className = "movement-copy";
        const name = document.createElement("strong");
        name.textContent = product.name;
        const time = document.createElement("span");
        time.textContent = movement.time;
        copy.append(name, time);
        const amount = document.createElement("span");
        amount.className = `movement-amount${movement.type === "out" ? " is-out" : ""}`;
        amount.textContent = `${movement.type === "in" ? "+" : "−"}${movement.quantity} un.`;
        item.append(copy, amount);
        return item;
    }));

    const ranking = [...products].sort((left, right) => right.movements - left.movements);
    const maxMovements = ranking[0].movements;
    const rankingList = document.querySelector("#ranking-list");
    rankingList.replaceChildren(...ranking.slice(0, 4).map((product, index) => {
        const item = document.createElement("li");
        item.className = "ranking-item";
        const number = document.createElement("span");
        number.className = "ranking-number";
        number.textContent = String(index + 1).padStart(2, "0");
        const copy = document.createElement("span");
        copy.className = "ranking-copy";
        const name = document.createElement("strong");
        name.textContent = product.name;
        const count = document.createElement("span");
        count.textContent = `${product.movements} movimentações`;
        copy.append(name, count);
        const bar = document.createElement("span");
        bar.className = "ranking-bar";
        const fill = document.createElement("span");
        fill.style.width = `${Math.round(product.movements / maxMovements * 100)}%`;
        bar.append(fill);
        item.append(number, copy, bar);
        return item;
    }));
}

function showProductDetails(product) {
    const state = getStockState(product);
    dialogContent.innerHTML = `
        <div class="dialog-product-image"><img src="${product.image}" alt=""></div>
        <p class="dialog-kicker">${product.category.toLocaleUpperCase("pt-BR")} · ${state.label.toLocaleUpperCase("pt-BR")}</p>
        <h2 class="dialog-title">${product.name}</h2>
        <p class="dialog-description">${product.description}</p>
        <dl class="dialog-facts">
            <div><dt>Local</dt><dd>${product.location}</dd></div>
            <div><dt>Quantidade</dt><dd>${product.quantity} unidades</dd></div>
            <div><dt>Estoque mínimo</dt><dd>${product.minimum} unidades</dd></div>
            <div><dt>Preço de venda</dt><dd>${currency.format(product.price)}</dd></div>
        </dl>`;
    dialog.showModal();
}

document.querySelectorAll("[data-filter]").forEach((button) => {
    button.addEventListener("click", () => {
        activeFilter = button.dataset.filter;
        document.querySelectorAll("[data-filter]").forEach((filterButton) => {
            const selected = filterButton === button;
            filterButton.classList.toggle("is-selected", selected);
            filterButton.setAttribute("aria-pressed", String(selected));
        });
        renderProducts();
    });
});

document.querySelectorAll("[data-view]").forEach((button) => {
    button.addEventListener("click", () => {
        const showInventory = button.dataset.view === "inventory";
        document.querySelector("#inventory-view").hidden = !showInventory;
        document.querySelector("#reports-view").hidden = showInventory;
        document.querySelectorAll("[data-view]").forEach((navButton) => {
            navButton.classList.toggle("is-current", navButton === button);
            if (navButton === button) navButton.setAttribute("aria-current", "page");
            else navButton.removeAttribute("aria-current");
        });
    });
});

searchInput.addEventListener("input", renderProducts);
document.addEventListener("keydown", (event) => {
    if (event.key === "/" && document.activeElement !== searchInput && !dialog.open) {
        event.preventDefault();
        searchInput.focus();
    }
});

updateSummary();
renderProducts();
renderReports();