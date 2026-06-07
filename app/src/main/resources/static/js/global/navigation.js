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
        // Exact
        if (currentPath === routePattern) return true;

        // Pattern match for routes with parameters
        if (routePattern.includes('{')) {
            const patternRegex = new RegExp('^' + routePattern.replace(/\{.*?\}/g, '[^/]+') + '$');
            return patternRegex.test(currentPath);
        }

        // Prefix match for nested routes
        if (currentPath.startsWith(routePattern + '/')) {
            return true;
        }

        return false;
    }

    setupNavigationEvents() {
        document.addEventListener('click', (e) => {
            const link = e.target.closest('[data-nav]');
            if (link) {
                e.preventDefault();
                const routeName = link.getAttribute('data-nav');
                const params = this.getDataParams(link);
                this.navigateTo(routeName, params);
            }
        });

        window.addEventListener('popstate',  (e) => {
            this.handlePopState(e);
        });
    }

    getDataParams(element) {
        const params = {};
        const dataAttributes = element.dataset;

        for (const [key, value] of Object.entries(dataAttributes)) {
            if (key.startsWith('param')) {
                const paramName = key.replace('param', '').toLowerCase();
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
        
        // Exact match
        if (currentPath === routePath) return true;
        
        // Pattern match for parameterized routes
        if (routePath.includes('{')) {
            const patternRegex = new RegExp('^' + routePath.replace(/\{.*?\}/g, '[^/]+') + '$');
            return patternRegex.test(currentPath);
        }
        
        // For admin routes, check if we're in admin section
//        if (routePath.startsWith('/admin') && currentPath.startsWith('/admin')) {
//            return true;
//        }
        
        return false;
    }

    navigateTo(routeName, data = {}) {
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
        
        // Replace route parameters
        if (url.includes('{') && data) {
            for (const [key, value] of Object.entries(data)) {
                const paramPattern = `{${key}}`;
                if (url.includes(paramPattern)) {
                    url = url.replace(paramPattern, value);
                    // Remove used parameter from data to avoid duplicate query params
                    delete data[key];
                }
            }
        }
        
        // Add query parameters
        if (Object.keys(data).length > 0) {
            const params = new URLSearchParams(data).toString();
            url += (url.includes('?') ? '&' : '?') + params;
        }
        
        return url;
    }

    shouldUseClientSideNavigation(routeName) {
        if (routeName.startsWith('admin')) return true;
        const spaPrefixes = ['profile', 'account', 'settings'];
        return spaPrefixes.some(prefix => routeName.startsWith(prefix));
    }

    clientSideNavigation(url, routeName, data) {
        fetch(url, {
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'text/html',
                'X-Navigation': 'client-side'
            }
        }).then(response => {
            if (response.ok) {
                return response.text();
            }
            throw new Error('Network response was not ok.');
        }).then(html => {
            this.updatePageContent(html, routeName);

            window.history.pushState({
                route: routeName,
                data: data,
                timestamp: Date.now()
            }, '', url);

            this.previousPage = this.currentPage;
            this.currentPage = routeName;
            this.setupActiveState();
            this.onRouteChange(routeName, data);
            return true;
        }).catch(error => {
            console.error('Navigation error:', error);
            this.serverSideNavigate(url); // Fallback to server navigation
            return false;
        }).finally(() => document.body.classList.remove('loading'));
    }

    serverSideNavigate(url) {
        window.location.href = url;
        return true;
    }

    updatePageContent(html, routeName) {
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
        this.setupNavigationEvents();

        window.dispatchEvent(new CustomEvent('pageContentUpdated', {
            detail: { route: this.currentPage, previousRoute: this.previousPage }
        }));
    }

    handlePopState(event) {
        if (event.state && event.state.route) {
            this.previousPage = this.currentPage;
            this.currentPage = event.state.route;
            this.setupActiveState();
            this.onRouteChange(this.currentPage, event.state.data || {});
        } else {
            this.detectCurrentPage();
            this.setupActiveState();
            this.onRouteChange(this.currentPage, {});
        }
    }

    onRouteChange(routeName, data) {
        console.log(`Navigated to: ${routeName}`, data);

        window.dispatchEvent(new CustomEvent('routeChanged', {
            detail: { route: routeName, data: data, previousRoute: this.previousPage, isAdminRoute: routeName.startsWith('admin') }
        }));

        this.previousPage = routeName;
    }

    redirectTo(routeName, data = {}) {
        return this.navigateTo(routeName, data);
    }

    reload() {
        window.location.reload();
    }

    goBack() {
        window.history.back();
    }

    goForward() {
        window.history.forward();
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
        const urlParams = new URLSearchParams(window.location.search);
        const params = {};

        for (const [key, value] of urlParams) {
            params[key] = value;
        }
        return params;
    }
}


const navigation = new NavigationManager();
