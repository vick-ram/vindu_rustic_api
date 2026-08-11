class NavigationManager {
    constructor() {
        this.currentPage = '';
        this.previousPage = '';
        this.routes = {};
        this.init();
    }

    init() {
        this.routes = {
            // Ecommerce routes
            home: '/',
            shop: '/shop',
            products: '/products',
            productDetail: '/products/{id}',
            categories: '/categories',
            cart: '/cart',
            checkout: '/checkout',
            orders: '/orders',
            orderTracking: '/orders/tracking',
            wishlist: '/wishlist',

            // Dashboard
            adminDashboard: '/admin/dashboard',
            adminCustomers: '/admin/users',
            adminCustomersDetail: '/admin/users/detail/{id}',
            adminCategories: '/admin/products/categories',
            adminProducts: '/admin/products/list',
            adminReviews: '/admin/products/reviews',
            adminProductsCreate: '/admin/products/new',
            adminOrders: '/admin/orders',
            adminAnalytics: '/admin/analytics',
            adminHelp: '/admin/help',
            adminSettings: '/admin/settings',

            // Account
            login: '/login',
            register: '/register',
        };

        this.detectCurrentPage();
        this.setupNavigationEvents();
        this.setupActiveState();
    }

    detectCurrentPage() {
        const path = window.location.pathname;
        this.currentPage = path;

        // Map path to route name for easier reference
        for (const [name, route] of Object.entries(this.routes)) {
            if (this.matchesRoute(path, route)) {
                this.currentPage = name;
                break;
            }
        }
    }


    matchesRoute(currentPath, routePattern) {
        if (currentPath === routePattern) return true;

        if (routePattern.includes('{')) {
            // Escape special regex chars except the parameterized placeholders
            const regexString = routePattern
                .replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
                .replace(/\\\{.*?\\\}/g, '[^/]+');

            return new RegExp('^' + regexString + '$').test(currentPath);
        }

        return false;
    }

    setupNavigationEvents() {
        document.addEventListener('click', async (e) => {
            const link = e.target.closest('[data-nav]');
            if (link) {
                e.preventDefault();
                const routeName = link.getAttribute('data-nav');
                const params = this.getDataParams(link);
                await this.navigateTo(routeName, params);
            }
        });

        window.addEventListener('popstate', async (e) => {
            await this.handlePopState(e);
        });
    }

    getDataParams(element) {
        const params = {};
        const dataAttributes = element.dataset;

        for (const [key, value] of Object.entries(dataAttributes)) {
            if (key.startsWith('param')) {
                const rawParam = key.replace(/^param/, '');
                const paramName = rawParam.charAt(0).toLowerCase() + rawParam.slice(1);
                console.log('Param:', paramName, value);
                params[paramName] = value;
            }
        }
        return params;
    }

    setupActiveState() {
        const navItems = document.querySelectorAll('[data-nav]');
        navItems.forEach(item => {
            const navTarget = item.getAttribute('data-nav');
            if (this.isActiveRoute(navTarget)) {
                item.classList.add('active');
                item.setAttribute('aria-current', 'page');
            } else {
                item.classList.remove('active');
                item.removeAttribute('aria-current');
            }
        });
    }

    isActiveRoute(routeName) {
        const routePath = this.routes[routeName];
        if (!routePath) return false;

        const currentPath = window.location.pathname;
        return this.matchesRoute(currentPath, routePath);
    }

   async navigateTo(routeName, data = {}) {
        if (!this.routes[routeName]) {
            console.error(`Route "${routeName}" not found.`);
            return false;
        }

        const url = this.buildUrl(routeName, data);

        if (this.shouldUseClientSideNavigation(routeName)) {
            return this.clientSideNavigation(url, routeName, data);
        } else {
            return this.serverSideNavigate(url);
        }
    }

    buildUrl(routeName, data = {}) {
        let url = this.routes[routeName];
        const paramCopy = {...data}
        // Replace route parameters
        if (url.includes('{')) {
            for (const [key, value] of Object.entries(paramCopy)) {
                const paramPattern = `{${key}}`;
                if (url.includes(paramPattern)) {
                    url = url.replace(paramPattern, encodeURIComponent(value));
                    // Remove used parameter from data to avoid duplicate query params
                    delete paramCopy[key];
                }
            }
        }

        // Add query parameters
        if (Object.keys(paramCopy).length > 0) {
            const queryString = new URLSearchParams(paramCopy).toString();
            url += (url.includes('?') ? '&' : '?') + queryString;
        }

        return url;
    }

    shouldUseClientSideNavigation(routeName) {
        if (routeName.startsWith('admin')) return true;
        const spaPrefixes = ['profile', 'account', 'settings'];
        return spaPrefixes.some(prefix => routeName.startsWith(prefix));
    }

    async clientSideNavigation(url, routeName, data, pushState = true) {
        document.body.classList.add('loading');
        try {
            const response = await fetch(url, {
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'text/html',
                    'X-Navigation': 'client-side'
                }
            });

            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            const html = await response.text();
            this.updatePageContent(html);

            if (pushState) {
                window.history.pushState({
                    route: routeName,
                    data: data,
                    timestamp: Date.now()
                }, '', url);
            }

            this.previousPage = this.currentPage;
            this.currentPage = routeName;
            this.setupActiveState();
            this.onRouteChange(routeName, data);
            return true;
        } catch (error) {
            console.error('Navigation error:', error);
            if (pushState) this.serverSideNavigate(url);
            return false;
        } finally {
            document.body.classList.remove('loading');
        }
    }

    serverSideNavigate(url) {
        window.location.href = url;
        return true;
    }

    updatePageContent(html) {
        const parser = new DOMParser();
        const newDoc = parser.parseFromString(html, 'text/html');

        const mainContent = document.querySelector('main, [data-content]');
        const newContent = newDoc.querySelector('main, [data-content]');

        if (mainContent && newContent) {
            mainContent.innerHTML = newContent.innerHTML;
        }

        // Update page title
        const newTitle = newDoc.querySelector('title');
        if (newTitle) {
            document.title = newTitle.textContent;
        }

        this.updateMetaTags(newDoc);

        // Re-initialize any dynamic content
        this.reinitializeDynamicContent();
    }

    updateMetaTags(newDoc) {
        const metaTags = ['description', 'keywords'];
        metaTags.forEach(name => {
            const newTag = newDoc.querySelector(`meta[name="${name}"]`);
            const currentTag = document.querySelector(`meta[name="${name}"]`);
            if (newTag && currentTag) {
                currentTag.content = newTag.content;
            }
        });
    }


    reinitializeDynamicContent() {
        window.dispatchEvent(new CustomEvent('pageContentUpdated', {
            detail: {route: this.currentPage, previousRoute: this.previousPage}
        }));
    }

    async handlePopState(event) {
        const currentUrl = window.location.pathname + window.location.search;
        let routeName = event.state?.route;

        if (!routeName) {
            this.detectCurrentPage();
            routeName = this.currentPage;
        }

        // Refetch/render DOM content on back/forward
        if (this.shouldUseClientSideNavigation(routeName)) {
            await this.clientSideNavigation(currentUrl, routeName, event.state?.data || {}, false);
        } else {
            this.serverSideNavigate(currentUrl);
        }
    }

    onRouteChange(routeName, data) {
        console.log(`Navigated to: ${routeName}`, data);

        window.dispatchEvent(new CustomEvent('routeChanged', {
            detail: {
                route: routeName,
                data: data,
                previousRoute: this.previousPage,
                isAdminRoute: routeName.startsWith('admin')
            }
        }));

        this.previousPage = routeName;
    }

    async redirectTo(routeName, data = {}) {
        return await this.navigateTo(routeName, data);
    }

    getCurrentRoute() {
        return {
            name: this.currentPage,
            path: this.routes[this.currentPage] || window.location.pathname,
            fullPath: window.location.href,
            params: this.getUrlParams(),
            isAdminRoute: this.currentPage.startsWith('admin')
        };
    }

    getUrlParams() {
        return Object.fromEntries(new URLSearchParams(window.location.search));
    }
}


const navigation = new NavigationManager();
