// Product data
const products = {
    all: [
        {
            id: 1,
            name: "Rustic Oak Bed Frame",
            price: "$1,299",
            description: "Handcrafted from solid oak with traditional joinery techniques.",
            image: "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?ixlib=rb-4.0.3&auto=format&fit=crop&w=1170&q=80",
            category: "beds"
        },
        {
            id: 2,
            name: "Farmhouse Dining Table",
            price: "$899",
            description: "A spacious dining table perfect for family gatherings.",
            image: "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?ixlib=rb-4.0.3&auto=format&fit=crop&w=1758&q=80",
            category: "tables"
        },
        {
            id: 3,
            name: "Handcrafted Windsor Chair",
            price: "$249",
            description: "Classic design with exceptional comfort and durability.",
            image: "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?ixlib=rb-4.0.3&auto=format&fit=crop&w=687&q=80",
            category: "chairs"
        },
        {
            id: 4,
            name: "Traditional Wooden Sink",
            price: "$1,599",
            description: "Custom-crafted wooden sink with copper fixtures.",
            image: "https://images.unsplash.com/photo-1556912165-f9af0f6d9ce1?ixlib=rb-4.0.3&auto=format&fit=crop&w=1074&q=80",
            category: "sinks"
        },
        {
            id: 5,
            name: "Antique Style Cabinet",
            price: "$1,199",
            description: "Distressed finish with ample storage space.",
            image: "https://images.unsplash.com/photo-1595428774223-ef52624120d2?ixlib=rb-4.0.3&auto=format&fit=crop&w=1074&q=80",
            category: "cabinets"
        },
        {
            id: 6,
            name: "Wrought Iron Chandelier",
            price: "$599",
            description: "Elegant lighting fixture with a rustic charm.",
            image: "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?ixlib=rb-4.0.3&auto=format&fit=crop&w=1170&q=80",
            category: "lighting"
        }
    ],
    furniture: [
        {
            id: 1,
            name: "Rustic Oak Bed Frame",
            price: "$1,299",
            description: "Handcrafted from solid oak with traditional joinery techniques.",
            image: "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?ixlib=rb-4.0.3&auto=format&fit=crop&w=1170&q=80",
            category: "beds"
        },
        {
            id: 2,
            name: "Farmhouse Dining Table",
            price: "$899",
            description: "A spacious dining table perfect for family gatherings.",
            image: "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?ixlib=rb-4.0.3&auto=format&fit=crop&w=1758&q=80",
            category: "tables"
        },
        {
            id: 3,
            name: "Handcrafted Windsor Chair",
            price: "$249",
            description: "Classic design with exceptional comfort and durability.",
            image: "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?ixlib=rb-4.0.3&auto=format&fit=crop&w=687&q=80",
            category: "chairs"
        },
        {
            id: 5,
            name: "Antique Style Cabinet",
            price: "$1,199",
            description: "Distressed finish with ample storage space.",
            image: "https://images.unsplash.com/photo-1595428774223-ef52624120d2?ixlib=rb-4.0.3&auto=format&fit=crop&w=1074&q=80",
            category: "cabinets"
        }
    ],
    lighting: [
        {
            id: 6,
            name: "Wrought Iron Chandelier",
            price: "$599",
            description: "Elegant lighting fixture with a rustic charm.",
            image: "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?ixlib=rb-4.0.3&auto=format&fit=crop&w=1170&q=80",
            category: "lighting"
        },
        {
            id: 7,
            name: "Rustic Table Lamp",
            price: "$199",
            description: "Hand-turned wooden base with linen shade.",
            image: "https://images.unsplash.com/photo-1558618666-fcd25856cd55?ixlib=rb-4.0.3&auto=format&fit=crop&w=687&q=80",
            category: "lighting"
        }
    ],
    decor: [
        {
            id: 8,
            name: "Handwoven Wall Tapestry",
            price: "$349",
            description: "Traditional patterns with natural dyes.",
            image: "https://images.unsplash.com/photo-1582582621959-48d27397dc69?ixlib=rb-4.0.3&auto=format&fit=crop&w=1169&q=80",
            category: "decor"
        },
        {
            id: 9,
            name: "Ceramic Vase Set",
            price: "$129",
            description: "Artisan-crafted with rustic glaze finishes.",
            image: "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?ixlib=rb-4.0.3&auto=format&fit=crop&w=687&q=80",
            category: "decor"
        }
    ],
    "kitchen-bath": [
        {
            id: 4,
            name: "Traditional Wooden Sink",
            price: "$1,599",
            description: "Custom-crafted wooden sink with copper fixtures.",
            image: "https://images.unsplash.com/photo-1556912165-f9af0f6d9ce1?ixlib=rb-4.0.3&auto=format&fit=crop&w=1074&q=80",
            category: "sinks"
        },
        {
            id: 10,
            name: "Butcher Block Countertop",
            price: "$2,199",
            description: "Solid maple countertop for kitchen or bathroom.",
            image: "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?ixlib=rb-4.0.3&auto=format&fit=crop&w=1170&q=80",
            category: "kitchen"
        }
    ],
    sale: [
        {
            id: 11,
            name: "Display Model Armchair",
            price: "$399",
            originalPrice: "$599",
            description: "Slightly used display model in perfect condition.",
            image: "https://images.unsplash.com/photo-1592078615290-033ee584e267?ixlib=rb-4.0.3&auto=format&fit=crop&w=1064&q=80",
            category: "chairs"
        },
        {
            id: 12,
            name: "Seasonal Clearance Lamp",
            price: "$129",
            originalPrice: "$199",
            description: "Limited stock - last season's design.",
            image: "https://images.unsplash.com/photo-1558618666-fcd25856cd55?ixlib=rb-4.0.3&auto=format&fit=crop&w=687&q=80",
            category: "lighting"
        }
    ]
};

// Page navigation functionality
document.addEventListener('DOMContentLoaded', function() {
    const navLinks = document.querySelectorAll('.nav-links a, .footer-links a, .logo, .btn-submit[data-page]');
    const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
    const navLinksContainer = document.querySelector('.nav-links');

    // Mobile menu toggle
    mobileMenuBtn.addEventListener('click', function() {
        navLinksContainer.classList.toggle('active');
    });

    // Page navigation
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();

            // Close mobile menu if open
            navLinksContainer.classList.remove('active');

            const pageId = this.getAttribute('data-page');
            if (pageId) {
                navigateToPage(pageId);
            }
        });
    });

    // Initialize the page
    navigateToPage('home');

    // Custom order form submission
    const customOrderForm = document.getElementById('custom-order-form');
    if (customOrderForm) {
        customOrderForm.addEventListener('submit', function(e) {
            e.preventDefault();
            alert('Thank you for your custom order request! We will contact you within 2 business days.');
            this.reset();
        });
    }
});

function navigateToPage(pageId) {
    // Hide all pages
    const pages = document.querySelectorAll('main section');
    pages.forEach(page => {
        page.style.display = 'none';
    });

    // Show the selected page
    const targetPage = document.getElementById(`${pageId}-page`);
    if (targetPage) {
        targetPage.style.display = 'block';

        // Update active navigation link
        document.querySelectorAll('.nav-links a').forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('data-page') === pageId) {
                link.classList.add('active');
            }
        });

        // Load products for the page
        loadProductsForPage(pageId);

        // Scroll to top
        window.scrollTo(0, 0);
    }
}

function loadProductsForPage(pageId) {
    const productsGrid = document.querySelector(`#${pageId}-page .products-grid`);
    if (!productsGrid) return;

    // Clear existing products
    productsGrid.innerHTML = '';

    // Get products for this page
    let pageProducts = [];
    if (pageId === 'home' || pageId === 'explore-collection') {
        pageProducts = products.all;
    } else {
        pageProducts = products[pageId] || [];
    }

    // Add products to the grid
    pageProducts.forEach(product => {
        const productCard = document.createElement('div');
        productCard.className = 'product-card';

        const saleBadge = product.originalPrice ?
        `<div style="position: absolute; top: 10px; right: 10px; background: #e74c3c; color: white; padding: 5px 10px; border-radius: 4px; font-size: 0.8rem;">SALE</div>` : '';

        const originalPrice = product.originalPrice ?
        `<span style="text-decoration: line-through; color: #999; margin-right: 10px;">${product.originalPrice}</span>` : '';

        productCard.innerHTML = `
                    <div class="product-image" style="position: relative;">
                        ${saleBadge}
                        <img src="${product.image}" alt="${product.name}">
                    </div>
                    <div class="product-info">
                        <h3 class="product-name">${product.name}</h3>
                        <div class="product-price">${originalPrice}${product.price}</div>
                        <p class="product-description">${product.description}</p>
                        <button class="btn-add-to-cart">Add to Cart</button>
                    </div>
                `;

        productsGrid.appendChild(productCard);
    });

    // Add event listeners to filter buttons
    const filterButtons = document.querySelectorAll(`#${pageId}-page .filter-btn`);
    filterButtons.forEach(button => {
        button.addEventListener('click', function() {
            // Update active filter button
            filterButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // Filter products
            const category = this.getAttribute('data-category');
            filterProducts(pageId, category);
        });
    });
}

function filterProducts(pageId, category) {
    const productsGrid = document.querySelector(`#${pageId}-page .products-grid`);
    if (!productsGrid) return;

    // Clear existing products
    productsGrid.innerHTML = '';

    // Get products for this page
    let pageProducts = [];
    if (pageId === 'home' || pageId === 'explore-collection') {
        pageProducts = products.all;
    } else {
        pageProducts = products[pageId] || [];
    }

    // Filter by category if not "all"
    if (category !== 'all') {
        pageProducts = pageProducts.filter(product => product.category === category);
    }

    // Add filtered products to the grid
    pageProducts.forEach(product => {
        const productCard = document.createElement('div');
        productCard.className = 'product-card';

        const saleBadge = product.originalPrice ?
        `<div style="position: absolute; top: 10px; right: 10px; background: #e74c3c; color: white; padding: 5px 10px; border-radius: 4px; font-size: 0.8rem;">SALE</div>` : '';

        const originalPrice = product.originalPrice ?
        `<span style="text-decoration: line-through; color: #999; margin-right: 10px;">${product.originalPrice}</span>` : '';

        productCard.innerHTML = `
                    <div class="product-image" style="position: relative;">
                        ${saleBadge}
                        <img src="${product.image}" alt="${product.name}">
                    </div>
                    <div class="product-info">
                        <h3 class="product-name">${product.name}</h3>
                        <div class="product-price">${originalPrice}${product.price}</div>
                        <p class="product-description">${product.description}</p>
                        <button class="btn-add-to-cart">Add to Cart</button>
                    </div>
                `;

        productsGrid.appendChild(productCard);
    });

    // Add event listeners to Add to Cart buttons
    const addToCartButtons = document.querySelectorAll(`#${pageId}-page .btn-add-to-cart`);
    addToCartButtons.forEach(button => {
        button.addEventListener('click', function() {
            const productName = this.closest('.product-card').querySelector('.product-name').textContent;
            alert(`Added ${productName} to your cart!`);

            // Update cart count
            const cartCount = document.querySelector('.cart-count');
            let count = parseInt(cartCount.textContent);
            cartCount.textContent = count + 1;
        });
    });
}