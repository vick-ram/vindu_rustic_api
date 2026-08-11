const Icons = {
    basePath: '/webjars/tabler-icons/2.40.0/icons/',
    iconCache: new Map(),
    iconMap: {
        'home': 'home',
        'user': 'user',
        'settings': 'settings',
        'search': 'search',
        'heart': 'heart',
        'star': 'star',
        'trash': 'trash',
        'edit': 'edit',
        'plus': 'plus',
        'minus': 'minus',
        'check': 'check',
        'x': 'x',
        'arrow-left': 'arrow-left',
        'arrow-right': 'arrow-right',
        'arrow-up': 'arrow-up',
        'arrow-down': 'arrow-down',
        'chevron-left': 'chevron-left',
        'chevron-right': 'chevron-right',
        'chevron-up': 'chevron-up',
        'chevron-down': 'chevron-down',
        'menu': 'menu-2',
        'bell': 'bell',
        'mail': 'mail',
        'message': 'message',
        'phone': 'phone',
        'map-pin': 'map-pin',
        'calendar': 'calendar',
        'clock': 'clock',
        'upload': 'upload',
        'download': 'download',
        'file': 'file',
        'folder': 'folder',
        'image': 'photo',
        'video': 'video',
        'music': 'music',
        'shopping-cart': 'shopping-cart',
        'credit-card': 'credit-card',
        'lock': 'lock',
        'unlock': 'lock-open',
        'eye': 'eye',
        'eye-off': 'eye-off',
        'link': 'link',
        'external-link': 'external-link',
        'share': 'share',
        'bookmark': 'bookmark',
        'flag': 'flag',
        'tag': 'tag',
        'filter': 'filter',
        'refresh': 'refresh',
        'rotate': 'rotate',
        'printer': 'printer',
        'save': 'device-floppy',
        'info': 'info-circle',
        'warning': 'alert-triangle',
        'error': 'alert-circle',
        'success': 'circle-check',
        'help': 'help-circle',
        'dashboard': 'dashboard',
        'chart': 'chart-bar',
        'table': 'table',
        'list': 'list',
        'grid': 'layout-grid',
        'layers': 'layers-difference',
        'copy': 'copy',
        'paste': 'clipboard',
        'cut': 'cut',
        'undo': 'arrow-back-up',
        'redo': 'arrow-forward-up',
        'zoom-in': 'zoom-in',
        'zoom-out': 'zoom-out',
        'maximize': 'maximize',
        'minimize': 'minimize',
        'fullscreen': 'arrows-maximize',
        'expand': 'arrows-diagonal',
        'compress': 'arrows-diagonal-minimize',
        'sun': 'sun',
        'moon': 'moon',
        'cloud': 'cloud',
        'rain': 'cloud-rain',
        'snow': 'cloud-snow',
        'wind': 'wind',
        'thermometer': 'thermometer',
        'wifi': 'wifi',
        'bluetooth': 'bluetooth',
        'battery': 'battery',
        'power': 'power'
    },

    /**
     * Get the file name for an icon
     * @param {string} iconName - The name of the icon
     * @returns {string} The file name
     */
    getFileName(iconName) {
        return this.iconMap[iconName] || iconName
    },

    /**
     * Build the ful path for an icon
     * @param {string} iconName - The name of the icon
     * @returns {string} The full path of the icon SVG
     */
    getIconPath(iconName) {
        const fileName = this.getFileName(iconName);
        return `${this.basePath}${fileName}.svg`;
    },

    /**
     * Fetch an SVG icon from the server
     * @param {string} iconName - The name of the icon
     * @returns {Promise<string>} The SVG content
     */
    async fetchIcon(iconName) {
        // check cache first
        if (this.iconCache.has(iconName)) {
            return this.iconCache.get(iconName);
        }

        try {
            const path = this.getIconPath(iconName);
            const response = await fetch(path);

            if (!response.ok) {
                throw new Error(`Failed to load icon: ${iconName}`);
            }

            const svgContent = await response.text();

            // cache the result
            this.iconCache.set(iconName, svgContent);

            return svgContent;
        } catch (error) {
            console.error(`Error loading icon ${iconName}:`, error);
            return this.getFallbackIcon(iconName);
        }
    },

    /**
     * Get a fallback icon when the requested icon fails to load
     * @param {string} iconName - The name of the icon that failed
     * @returns {string} A simple fallback SVG
     */
    getFallbackIcon(iconName) {
        return `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <line x1="12" y1="8" x2="12" y2="16"/>
            <line x1="8" y1="12" x2="16" y2="12"/>
        </svg>`;
    },

    /**
     * Create an icon element with optional customizations
     * @param {string} iconName - The name of the icon
     * @param {Object} options - customization options
     * @param {string} options.size - Size in px (default: 24)
     * @param {string} options.color - CSS color value
     * @param {string} options.class - CSS classes to add
     * @param {string} options.stroke - Stroke width (default: 2)
     * @returns {HTMLElement} The icon element
     */
    async createIcon(iconName, options = {}) {
        const svgContent = await this.fetchIcon(iconName);

        // create a temporary container to parse the svg
        const temp = document.createElement('div');
        temp.innerHTML = svgContent;
        const svgElement = temp.querySelector('svg');

        if (!svgElement) {
            console.error(`Invalid SVG for icon: ${iconName}`);
            return this.createPlaceholder();
        }

        const size = options.size || 24;
        svgElement.setAttribute('width', size);
        svgElement.setAttribute('height', size);

        if (options.color) {
            svgElement.style.color = options.color;
        }

        if (options.stroke) {
            svgElement.setAttribute('stroke-width', options.stroke);
        }

        if (options.class) {
            svgElement.classList.add(...options.class.split(' '));
        }

        // add base classes
        svgElement.classList.add('ti', `ti-${iconName}`);

        return svgElement;
    },

    /**
     * Render an icon onto a container element
     * @param {string} iconName - The name of the icon
     * @param {string|HTMLElement} container - Container selector or element
     * @param {Object} options - Customization options
     * @returns {Promise<void>}
     */
    async renderIcon(iconName, container, options = {}) {
        const iconElement = await this.createIcon(iconName, options);

        let targetContainer;
        if (typeof container === 'string') {
            targetContainer = document.querySelector(container);
        } else {
            targetContainer = container;
        }

        if (targetContainer) {
            targetContainer.innerHTML = '';
            targetContainer.appendChild(iconElement);
        } else {
            console.error('Container not found:', container);
        }
    },

    /**
     * Create a placeholder element
     * @returns {HTMLElement}
     */
    createPlaceholder() {
        const placeholder = document.createElement('span');
        placeholder.classList.add('icon-placeholder');
        placeholder.innerHTML = '?';
        return placeholder;
    },

    /**
     * Preload commonly used icons
     * @param {string[]} iconNames - Array of icon names to preload
     * @returns {Promise<void>}
     */
    async preloadIcons(iconNames) {
        const promises = iconNames.map(iconName => this.fetchIcon(iconName));
        await Promise.all(promises);
    },

    /**
     * Initialize and scan the page for icon placeholders
     * This function looks for elements with 'data-icon' attribute and renders icons
     */
    async init() {
        const iconElements = document.querySelectorAll('[data-icon]');
        const iconPromises = [];

        iconElements.forEach(element => {
            const iconName = element.dataset.iconName;
            const options = {
                size: element.dataset.size || 24,
                color: element.dataset.color,
                class: element.dataset.class,
                stroke: element.dataset.stroke
            };
            iconPromises.push(this.renderIcon(iconName, element, options));
        });

        await Promise.all(iconPromises);
    }
};

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        Icons.init();
    });
} else {
    Icons.init();
}