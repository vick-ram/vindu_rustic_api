/**
 * Lightweight, Zero-Dependency On-The-Fly Utility CSS Engine
 * Production-Ready
 */
(function () {
    'use strict';

    const CONFIG = {
        theme: {
            light: {
                'primary': '#6F370F',
                'secondary': '#C49A6C',
                'accent': '#B85B14',
                'background': '#F9F6F0',
                'surface': '#FFFFFF',
                'surface-alt': '#F0EAE1',
                'text': '#3D2B1F',
                'text-secondary': '#6D5446',
                'text-muted': '#7A6254',
                'border': '#D7C9B8',
                'success': '#557A32',
                'warning': '#C67D00',
                'error': '#C62828',
                'info': '#4A6B6C',
                'container': '#FFFFFF',
                'hover': 'rgba(111, 55, 15, 0.06)',
                'overlay': 'rgba(30, 20, 15, 0.6)',
                'blue-primary': 'rgb(59, 130, 246)',
            },
            dark: {
                'primary': '#D98236',
                'secondary': '#8C6247',
                'accent': '#E09A5D',
                'background': '#17120E',
                'surface': '#241D18',
                'surface-alt': '#302620',
                'text': '#F3ECE7',
                'text-secondary': '#CBBBB0',
                'text-muted': '#A39083',
                'border': '#44352C',
                'success': '#82B35B',
                'warning': '#FFA000',
                'error': '#EF5350',
                'info': '#6B9092',
                'container': '#241D18',
                'hover': 'rgba(217, 130, 54, 0.12)',
                'overlay': 'rgba(0, 0, 0, 0.75)',
                'blue-primary': 'rgb(29, 78, 216)',
            },
        },
        breakpoints: {
            sm: '640px',
            md: '768px',
            lg: '1024px',
            xl: '1280px',
            '2xl': '1536',
        },
        spacingUnit: 0.25,
        fontSizes: {
            xs: '0.75rem',
            sm: '0.875rem',
            base: '1rem',
            lg: '1.125rem',
            xl: '1.25rem',
            '2xl': '1.5rem',
            '3xl': '1.875rem',
            '4xl': '2.25rem',
            '5xl': '3rem',
            '6xl': '3.75rem',
            '7xl': '4.5rem',
            '8xl': '6rem',
            '9xl': '8rem',
        },
        fontFamily: {
            'sans': 'ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
            'serif': 'ui-serif, Georgia, Cambria, "Times New Roman", Times, serif',
            'mono': 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
        },
        lineHeight: {
            'none': '1',
            'tight': '1.25',
            'snug': '1.375',
            'normal': '1.5',
            'relaxed': '1.625',
            'loose': '2',
        },
        letterSpacing: {
            'tighter': '-0.05em',
            'tight': '-0.025em',
            'normal': '0em',
            'wide': '0.025em',
            'wider': '0.05em',
            'widest': '0.1em',
        },
        borderRadius: {
            '': '0.25rem',
            'none': '0px',
            sm: '0.125rem',
            md: '0.375rem',
            lg: '0.5rem',
            xl: '0.75rem',
            '2xl': '1rem',
            '3xl': '1.5rem',
            full: '9999px',
        },
        shadows: {
            '': '0 1px 3px 0 rgba(0,0,0,0.1), 0 1px 2px 0 rgba(0,0,0,0.06)',
            sm: '0 1px 2px 0 rgba(0,0,0,0.05)',
            md: '0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06)',
            lg: '0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05)',
            xl: '0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04)',
            '2xl': '0 25px 50px -12px rgba(0,0,0,0.25)',
            inner: 'inset 0 2px 4px 0 rgba(0,0,0,0.06)',
            none: 'none',
        },
        maxWidth: {
            xs: '20rem',
            sm: '24rem',
            md: '28rem',
            lg: '32rem',
            xl: '36rem',
            '2xl': '42rem',
            '3xl': '48rem',
            '4xl': '56rem',
            '5xl': '64rem',
            '6xl': '72rem',
            '7xl': '80rem',
            full: '100%',
            min: 'min-content',
            max: 'max-content',
            fit: 'fit-content',
        },
        cursor: {
            'auto': 'auto',
            'default': 'default',
            'pointer': 'pointer',
            'wait': 'wait',
            'text': 'text',
            'move': 'move',
            'help': 'help',
            'not-allowed': 'not-allowed',
            'none': 'none',
            'grab': 'grab',
            'grabbing': 'grabbing',
        },
    };

    const state = {
        styleElement: null,
        styleSheet: null,
        processedClasses: new Set(),
        observer: null,
        isInitialized: false,
        currentTheme: 'light',
    };

    function getCurrentTheme() {
        const htmlElement = document.documentElement;
        return htmlElement.getAttribute('data-theme') || 'light';
    }

    function resolveThemeColor(token, isDark = false) {
        const colorMap = isDark ? CONFIG.theme.dark : CONFIG.theme.light;

        if (token.startsWith('gradient-')) {
            const gradientName = token.slice(9);
            if (isDark) {
                const gradients = {
                    'primary': 'linear-gradient(135deg, #D98236, #E09A5D)',
                    'secondary': 'linear-gradient(135deg, #8C6247, #241D18)',
                    'blue': 'linear-gradient(135deg, rgb(29, 78, 216), #E09A5D)',
                    'success': 'linear-gradient(135deg, #82B35B, #9CCC65)',
                    'error': 'linear-gradient(135deg, #EF5350, #EF5350)',
                    'warning': 'linear-gradient(135deg, #FFA000, #FFD54F)',
                };
                return gradients[gradientName];
            } else {
                const gradients = {
                    'primary': 'linear-gradient(135deg, #6F370F, #B85B14)',
                    'secondary': 'linear-gradient(135deg, #C49A6C, #FFFFFF)',
                    'blue': 'linear-gradient(135deg, rgb(59, 130, 246), #B85B14)',
                    'success': 'linear-gradient(135deg, #557A32, #8BC34A)',
                    'error': 'linear-gradient(135deg, #C62828, #E57373)',
                    'warning': 'linear-gradient(135deg, #C67D00, #FFCA28)',
                };
                return gradients[gradientName];
            }
        }
        return colorMap[token] || null;
    }

    function resolveColor(token) {
        if (!token) return null;

        if (token.startsWith('[') && token.endsWith(']')) {
            return token.slice(1, -1).replace(/_/g, ' ');
        }

        // Check for opacity modifier
        const opacityMatch = token.match(/^(.+?)\/(\d+)$/);

        if (opacityMatch) {
            const baseColor = opacityMatch[1];
            const opacityValue = opacityMatch[2];

            // Resolve the base color without opacity
            const resolvedColor = resolveColor(baseColor);
            if (resolvedColor) {
                const c =  applyOpacity(resolvedColor, opacityValue);
                console.log("Resolved color from opacity: ", c);
                return c;
            }
            return null;
        }

        const themeColor  = resolveThemeColor(token, state.currentTheme === 'dark');
        if (themeColor) return themeColor;

        // Standard colors
        if (token === 'white') return '#ffffff';
        if (token === 'black') return '#000000';
        if (token === 'transparent') return 'transparent';
        if (token === 'current') return 'currentColor';
        if (token === 'inherit') return 'inherit';

        const parts = token.split('-');
        if (parts.length >= 2) {
            const colorName = parts.slice(0, -1).join('-');
            const shade = parts[parts.length - 1];

            const standardColors = {
                'slate': { 50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1', 400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155', 800: '#1e293b', 900: '#0f172a' },
                'gray': { 50: '#f9fafb', 100: '#f3f4f6', 200: '#e5e7eb', 300: '#d1d5db', 400: '#9ca3af', 500: '#6b7280', 600: '#4b5563', 700: '#374151', 800: '#1f2937', 900: '#111827' },
                'red': { 50: '#fef2f2', 100: '#fee2e2', 200: '#fecaca', 300: '#fca5a5', 400: '#f87171', 500: '#ef4444', 600: '#dc2626', 700: '#b91c1c', 800: '#991b1b', 900: '#7f1d1d' },
                'blue': { 50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd', 400: '#60a5fa', 500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8', 800: '#1e40af', 900: '#1e3a8a' },
                'green': { 50: '#f0fdf4', 100: '#dcfce7', 200: '#bbf7d0', 300: '#86efac', 400: '#4ade80', 500: '#22c55e', 600: '#16a34a', 700: '#15803d', 800: '#166534', 900: '#14532d' },
                'yellow': { 50: '#fefce8', 100: '#fef9c3', 200: '#fef08a', 300: '#fde047', 400: '#facc15', 500: '#eab308', 600: '#ca8a04', 700: '#a16207', 800: '#854d0e', 900: '#713f12' },
                'indigo': { 50: '#eef2ff', 100: '#e0e7ff', 200: '#c7d2fe', 300: '#a5b4fc', 400: '#818cf8', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca', 800: '#3730a3', 900: '#312e81' },
                'purple': { 50: '#faf5ff', 100: '#f3e8ff', 200: '#e9d5ff', 300: '#d8b4fe', 400: '#c084fc', 500: '#a855f7', 600: '#9333ea', 700: '#7e22ce', 800: '#6b21a8', 900: '#581c87' },
                'pink': { 50: '#fdf2f8', 100: '#fce7f3', 200: '#fbcfe8', 300: '#f9a8d4', 400: '#f472b6', 500: '#ec4899', 600: '#db2777', 700: '#be185d', 800: '#9d174d', 900: '#831843' },
            };

            if (standardColors[colorName] && standardColors[colorName][shade]) {
                return standardColors[colorName][shade];
            }
        }
        return null;
    }

    function resolveSpacing(token) {
        if (!token) return null;

        if (token.startsWith('[') && token.endsWith(']')) {
            return token.slice(1, -1).replace(/_/g, ' ');
        }
        if (token === 'px') return '1px';
        if (token === 'full') return '100%';
        if (token === 'screen') return '100vw';
        if (token === 'min') return 'min-content';
        if (token === 'max') return 'max-content';
        if (token === 'fit') return 'fit-content';
        if (token.startsWith('-')) {
            const num = parseFloat(token.slice(1));
            return isNaN(num) ? null : `${num * -CONFIG.spacingUnit}rem`;
        }
        const num = parseFloat(token);
        return isNaN(num) ? null : `${num * CONFIG.spacingUnit}rem`;
    }

    function parseVariants(className) {
        const variants = [];
        const tokens = className.split(':');
        const pseudoClasses = [
            'hover', 'focus', 'active', 'visited', 'disabled', 'checked',
            'first-child', 'last-child', 'first-of-type', 'last-of-type',
            'only-child', 'only-of-type', 'empty', 'read-only', 'read-write',
            'placeholder-shown', 'autofill', 'required', 'valid', 'invalid',
            'in-range', 'out-of-range', 'focus-within', 'focus-visible',
            'target', 'default', 'indeterminate', 'optional', 'enabled'
        ];

        for (let i = 0; i < tokens.length - 1; i++) {
            const token = tokens[i];

            if (CONFIG.breakpoints[token]) {
                variants.push({type: 'breakpoint', value: token});
                continue;
            }

            // Check for pseudo-class variants
            if (pseudoClasses.includes(token)) {
                variants.push({type: 'pseudo', value: token});
                continue;
            }

            if (token === 'group-hover') {
                variants.push({type: 'pseudo', value: 'hover', parent: true});
                continue;
            }

            if (token === 'group-focus') {
                variants.push({type: 'pseudo', value: 'focus', parent: true});
                continue;
            }

            if (token === 'dark') {
                variants.push({type: 'dark'});
                continue;
            }

            return null;
        }

        return {
            base: tokens[tokens.length - 1],
            variants,
        };
    }

    function resolveDisplay(base) {
        const displayMap = {
            'block': 'block',
            'inline-block': 'inline-block',
            'inline': 'inline',
            'flex': 'flex',
            'inline-flex': 'inline-flex',
            'grid': 'grid',
            'inline-grid': 'inline-grid',
            'hidden': 'none',
            'contents': 'contents',
            'list-item': 'list-item',
            'table': 'table',
            'inline-table': 'inline-table',
            'table-caption': 'table-caption',
            'table-cell': 'table-cell',
            'table-column': 'table-column',
            'table-column-group': 'table-column-group',
            'table-footer-group': 'table-footer-group',
            'table-header-group': 'table-header-group',
            'table-row-group': 'table-row-group',
            'table-row': 'table-row',
            'flow-root': 'flow-root',
        };
        return displayMap[base] ? `display: ${displayMap[base]}` : null;
    }

    function resolveFlexbox(base) {
        const flexMap = {
            // Flex direction
            'flex-row': 'flex-direction: row',
            'flex-row-reverse': 'flex-direction: row-reverse',
            'flex-col': 'flex-direction: column',
            'flex-col-reverse': 'flex-direction: column-reverse',
            // Flex wrap
            'flex-wrap': 'flex-wrap: wrap',
            'flex-wrap-reverse': 'flex-wrap: wrap-reverse',
            'flex-nowrap': 'flex-wrap: nowrap',
            // Flex
            'flex-1': 'flex: 1 1 0%',
            'flex-auto': 'flex: 1 1 auto',
            'flex-initial': 'flex: 0 1 auto',
            'flex-none': 'flex: none',
            // Justify content
            'justify-start': 'justify-content: flex-start',
            'justify-end': 'justify-content: flex-end',
            'justify-center': 'justify-content: center',
            'justify-between': 'justify-content: space-between',
            'justify-around': 'justify-content: space-around',
            'justify-evenly': 'justify-content: space-evenly',
            // Align items
            'items-start': 'align-items: flex-start',
            'items-end': 'align-items: flex-end',
            'items-center': 'align-items: center',
            'items-baseline': 'align-items: baseline',
            'items-stretch': 'align-items: stretch',
            // Align content
            'content-start': 'align-content: flex-start',
            'content-end': 'align-content: flex-end',
            'content-center': 'align-content: center',
            'content-between': 'align-content: space-between',
            'content-around': 'align-content: space-around',
            'content-stretch': 'align-content: stretch',
            // Align self
            'self-auto': 'align-self: auto',
            'self-start': 'align-self: flex-start',
            'self-end': 'align-self: flex-end',
            'self-center': 'align-self: center',
            'self-stretch': 'align-self: stretch',
            'self-baseline': 'align-self: baseline',
            // Order
            'order-1': 'order: 1',
            'order-2': 'order: 2',
            'order-3': 'order: 3',
            'order-4': 'order: 4',
            'order-5': 'order: 5',
            'order-6': 'order: 6',
            'order-7': 'order: 7',
            'order-8': 'order: 8',
            'order-9': 'order: 9',
            'order-10': 'order: 10',
            'order-11': 'order: 11',
            'order-12': 'order: 12',
            'order-first': 'order: -9999',
            'order-last': 'order: 9999',
            'order-none': 'order: 0',
        };

        if (flexMap[base]) return flexMap[base];

        // Gap
        if (base === 'gap-0') return 'gap: 0';
        if (base.startsWith('gap-')) {
            const value = resolveSpacing(base.slice(4));
            return value ? `gap: ${value}` : null;
        }
        if (base === 'gap-x-0') return 'column-gap: 0px';
        if (base.startsWith('gap-x-')) {
            const value = resolveSpacing(base.slice(6));
            return value ? `column-gap: ${value}` : null;
        }
        if (base === 'gap-y-0') return 'row-gap: 0px';
        if (base.startsWith('gap-y-')) {
            const value = resolveSpacing(base.slice(6));
            return value ? `row-gap: ${value}` : null;
        }

        return null;
    }

    function resolveGrid(base) {
        if (base.startsWith('grid-cols-')) {
            const value = base.slice(10);
            if (value === 'none') return 'grid-template-columns: none';
            const num = parseInt(value);
            if (!isNaN(num) && num > 0 && num <= 12) {
                return `grid-template-columns: repeat(${num}, minmax(0, 1fr))`;
            }
        }

        // Grid template rows
        if (base.startsWith('grid-rows-')) {
            const value = base.slice(10);
            if (value === 'none') return 'grid-template-rows: none';
            const num = parseInt(value);
            if (!isNaN(num) && num > 0 && num <= 6) {
                return `grid-template-rows: repeat(${num}, minmax(0, 1fr))`;
            }
        }

        // Grid column span
        if (base.startsWith('col-span-')) {
            const value = base.slice(9);
            const num = parseInt(value);
            if (!isNaN(num) && num > 0 && num <= 12) {
                return `grid-column: span ${num} / span ${num}`;
            }
        }
        if (base === 'col-auto') return 'grid-column: auto';
        if (base === 'col-span-full') return 'grid-column: 1 / -1';

        // Grid row span
        if (base.startsWith('row-span-')) {
            const value = base.slice(9);
            const num = parseInt(value);
            if (!isNaN(num) && num > 0 && num <= 6) {
                return `grid-row: span ${num} / span ${num}`;
            }
        }
        if (base === 'row-auto') return 'grid-row: auto';
        if (base === 'row-span-full') return 'grid-row: 1 / -1';

        // Grid auto flow
        if (base === 'grid-flow-row') return 'grid-auto-flow: row';
        if (base === 'grid-flow-col') return 'grid-auto-flow: column';
        if (base === 'grid-flow-row-dense') return 'grid-auto-flow: row dense';
        if (base === 'grid-flow-col-dense') return 'grid-auto-flow: column dense';

        // Place items
        if (base === 'place-items-start') return 'place-items: start';
        if (base === 'place-items-end') return 'place-items: end';
        if (base === 'place-items-center') return 'place-items: center';
        if (base === 'place-items-stretch') return 'place-items: stretch';

        // Place content
        if (base === 'place-content-start') return 'place-content: start';
        if (base === 'place-content-end') return 'place-content: end';
        if (base === 'place-content-center') return 'place-content: center';
        if (base === 'place-content-between') return 'place-content: space-between';
        if (base === 'place-content-around') return 'place-content: space-around';
        if (base === 'place-content-evenly') return 'place-content: space-evenly';
        if (base === 'place-content-stretch') return 'place-content: stretch';

        // Place self
        if (base === 'place-self-auto') return 'place-self: auto';
        if (base === 'place-self-start') return 'place-self: start';
        if (base === 'place-self-end') return 'place-self: end';
        if (base === 'place-self-center') return 'place-self: center';
        if (base === 'place-self-stretch') return 'place-self: stretch';

        return null;
    }

    function resolveSpacingUtilities(base) {
        // Padding
        if (base === 'p-auto') return 'padding: auto';
        if (base.startsWith('p-')) {
            const value = resolveSpacing(base.slice(2));
            return value ? `padding: ${value}` : null;
        }
        if (base.startsWith('px-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `padding-left: ${value}; padding-right: ${value}` : null;
        }
        if (base.startsWith('py-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `padding-top: ${value}; padding-bottom: ${value}` : null;
        }
        if (base.startsWith('pt-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `padding-top: ${value}` : null;
        }
        if (base.startsWith('pr-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `padding-right: ${value}` : null;
        }
        if (base.startsWith('pb-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `padding-bottom: ${value}` : null;
        }
        if (base.startsWith('pl-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `padding-left: ${value}` : null;
        }

        // Margin
        if (base === 'm-auto') return 'margin: auto';
        if (base.startsWith('m-')) {
            const value = resolveSpacing(base.slice(2));
            return value ? `margin: ${value}` : null;
        }
        if (base === 'mx-auto') return 'margin: 0 auto';
        if (base.startsWith('mx-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `margin-left: ${value}; margin-right: ${value}` : null;
        }
        if (base.startsWith('my-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `margin-top: ${value}; margin-bottom: ${value}` : null;
        }
        if (base.startsWith('mt-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `margin-top: ${value}` : null;
        }
        if (base.startsWith('mr-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `margin-right: ${value}` : null;
        }
        if (base.startsWith('mb-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `margin-bottom: ${value}` : null;
        }
        if (base.startsWith('ml-')) {
            const value = resolveSpacing(base.slice(3));
            return value ? `margin-left: ${value}` : null;
        }

        // Space between (margin on children)
        if (base.startsWith('space-x-')) {
            const value = resolveSpacing(base.slice(8));
            if (value) {
                return `& > * + * { margin-left: ${value}; }`;
            }
        }
        if (base.startsWith('space-y-')) {
            const value = resolveSpacing(base.slice(8));
            if (value) {
                return `& > * + * { margin-top: ${value}; }`;
            }
        }

        return null;
    }

    function resolveSizing(base) {
        // Size (both width and height)
        if (base === 'size-auto') return 'width: auto; height: auto';
        if (base === 'size-full') return 'width: 100%; height: 100%';
        if (base === 'size-screen') return 'width: 100vw; height: 100vh';
        if (base === 'size-min') return 'width: min-content; height: min-content';
        if (base === 'size-max') return 'width: max-content; height: max-content';
        if (base === 'size-fit') return 'width: fit-content; height: fit-content';
        if (base.startsWith('size-')) {
            const value = resolveSpacing(base.slice(5));
            return value ? `width: ${value}; height: ${value}` : null;
        }

        if (base === 'w-auto') return 'width: auto';
        if (base === 'w-full') return 'width: 100%';
        if (base === 'w-screen') return 'width: 100vw';
        if (base === 'w-min') return 'width: min-content';
        if (base === 'w-max') return 'width: max-content';
        if (base === 'w-fit') return 'width: fit-content';
        if (base.startsWith('w-')) {
            const value = resolveSpacing(base.slice(2));
            return value ? `width: ${value}` : null;
        }

        if (base === 'min-w-0') return 'min-width: 0px';
        if (base === 'min-w-full') return 'min-width: 100%';
        if (base === 'min-w-min') return 'min-width: min-content';
        if (base === 'min-w-max') return 'min-width: max-content';
        if (base === 'min-w-fit') return 'min-width: fit-content';

        if (base === 'max-w-none') return 'max-width: none';
        if (base === 'max-w-full') return 'max-width: 100%';
        if (base === 'max-w-min') return 'max-width: min-content';
        if (base === 'max-w-max') return 'max-width: max-content';
        if (base === 'max-w-fit') return 'max-width: fit-content';
        if (base.startsWith('max-w-')) {
            const key = base.slice(6);
            return CONFIG.maxWidth[key] ? `max-width: ${CONFIG.maxWidth[key]}` : null;
        }

        if (base === 'h-auto') return 'height: auto';
        if (base === 'h-full') return 'height: 100%';
        if (base === 'h-screen') return 'height: 100vh';
        if (base === 'h-min') return 'height: min-content';
        if (base === 'h-max') return 'height: max-content';
        if (base === 'h-fit') return 'height: fit-content';
        if (base.startsWith('h-')) {
            const value = resolveSpacing(base.slice(2));
            return value ? `height: ${value}` : null;
        }

        if (base === 'min-h-0') return 'min-height: 0px';
        if (base === 'min-h-full') return 'min-height: 100%';
        if (base === 'min-h-screen') return 'min-height: 100vh';
        if (base === 'min-h-min') return 'min-height: min-content';
        if (base === 'min-h-max') return 'min-height: max-content';
        if (base === 'min-h-fit') return 'min-height: fit-content';
        if (base.startsWith('min-h-')) {
            const value = resolveSpacing(base.slice(6));
            return value ? `min-height: ${value}` : null;
        }

        if (base === 'max-h-full') return 'max-height: 100%';
        if (base === 'max-h-screen') return 'max-height: 100vh';
        if (base === 'max-h-min') return 'max-height: min-content';
        if (base === 'max-h-max') return 'max-height: max-content';
        if (base === 'max-h-fit') return 'max-height: fit-content';
        if (base.startsWith('max-h-')) {
            const value = resolveSpacing(base.slice(6));
            return value ? `max-height: ${value}` : null;
        }

        return null;
    }

    function resolveColors(base) {
        if (base.startsWith('bg-')) {
            if (base.startsWith('bg-gradient-')) {
                const color = resolveColor(base.slice(3));
                return color ? `background-image: ${color}` : null;
            }
            const color = resolveColor(base.slice(3));
            return color ? `background-color: ${color}` : null;
        }

        if (base.startsWith('text-')) {
            const size = CONFIG.fontSizes[base.slice(5)];
            if (size) return `font-size: ${size}`;

            const color = resolveColor(base.slice(5));
            return color ? `color: ${color}` : null;
        }

        if (base.startsWith('border-')) {
            const color = resolveColor(base.slice(7));
            if (color) return `border-color: ${color}`;

            const borderWidths = {'0': '0px', '2': '2px', '4': '4px', '8': '8px'};
            if (borderWidths[base.slice(7)]) {
                return `border-width: ${borderWidths[base.slice(7)]}`;
            }

            return null;
        }

        // Fill color for SVG
        if (base.startsWith('fill-')) {
            const color = resolveColor(base.slice(5));
            return color ? `fill: ${color}` : null;
        }

        // Stroke color for SVG
        if (base.startsWith('stroke-')) {
            const color = resolveColor(base.slice(7));
            return color ? `stroke: ${color}` : null;
        }

        return null;
    }

    function resolveBordersAndEffects(base) {
        if (base.startsWith('rounded')) {
            const key = base === 'rounded' ? '' : base.replace(/^rounded-?/, '');
            const radius = CONFIG.borderRadius[key] || resolveSpacing(key);
            return radius ? `border-radius: ${radius}` : null;
        }

        if (base === 'border') return 'border-width: 1px; border-style: solid';
        if (base === 'border-0') return 'border-width: 0px';
        if (base === 'border-2') return 'border-width: 2px';
        if (base === 'border-4') return 'border-width: 4px';
        if (base === 'border-8') return 'border-width: 8px';

        // Border style
        if (base === 'border-solid') return 'border-style: solid';
        if (base === 'border-dashed') return 'border-style: dashed';
        if (base === 'border-dotted') return 'border-style: dotted';
        if (base === 'border-double') return 'border-style: double';
        if (base === 'border-none') return 'border-style: none';

        // Individual border sides
        if (base === 'border-t') return 'border-top-width: 1px';
        if (base === 'border-r') return 'border-right-width: 1px';
        if (base === 'border-b') return 'border-bottom-width: 1px';
        if (base === 'border-l') return 'border-left-width: 1px';

        if (base.startsWith('shadow')) {
            const key = base.slice(6);
            return `box-shadow: ${CONFIG.shadows[key] || CONFIG.shadows['']}`;
        }

        // Opacity
        if (base.startsWith('opacity-')) {
            const value = parseFloat(base.slice(8));
            if (!isNaN(value) && value >= 0 && value <= 100) {
                return `opacity: ${value / 100}`;
            }
        }

        // Filters
        if (base === 'blur-none') return 'filter: blur(0)';
        if (base === 'blur-sm') return 'filter: blur(4px)';
        if (base === 'blur') return 'filter: blur(8px)';
        if (base === 'blur-md') return 'filter: blur(12px)';
        if (base === 'blur-lg') return 'filter: blur(16px)';
        if (base === 'blur-xl') return 'filter: blur(24px)';
        if (base === 'blur-2xl') return 'filter: blur(40px)';
        if (base === 'blur-3xl') return 'filter: blur(64px)';

        if (base.startsWith('brightness-')) {
            const value = parseFloat(base.slice(11));
            if (!isNaN(value)) return `filter: brightness(${value})`;
        }

        if (base.startsWith('contrast-')) {
            const value = parseFloat(base.slice(9));
            if (!isNaN(value)) return `filter: contrast(${value})`;
        }

        if (base.startsWith('grayscale')) {
            if (base === 'grayscale') return 'filter: grayscale(100%)';
            if (base === 'grayscale-0') return 'filter: grayscale(0)';
        }

        if (base.startsWith('sepia')) {
            if (base === 'sepia') return 'filter: sepia(100%)';
            if (base === 'sepia-0') return 'filter: sepia(0)';
        }

        // Object fit
        if (base === 'object-contain') return 'object-fit: contain';
        if (base === 'object-cover') return 'object-fit: cover';
        if (base === 'object-fill') return 'object-fit: fill';
        if (base === 'object-none') return 'object-fit: none';
        if (base === 'object-scale-down') return 'object-fit: scale-down';

        // Mix blend mode
        if (base === 'mix-blend-normal') return 'mix-blend-mode: normal';
        if (base === 'mix-blend-multiply') return 'mix-blend-mode: multiply';
        if (base === 'mix-blend-screen') return 'mix-blend-mode: screen';
        if (base === 'mix-blend-overlay') return 'mix-blend-mode: overlay';

        return null;
    }

    function resolveTypography(base) {
        if (base.startsWith('font-')) {
            const weightMap = {
                'thin': '100',
                'extralight': '200',
                'light': '300',
                'normal': '400',
                'medium': '500',
                'semibold': '600',
                'bold': '700',
                'extrabold': '800',
                'black': '900',
            };

            const weight = weightMap[base.slice(5)];
            if (weight) return `font-weight: ${weight}`;

            const fontFamily = CONFIG.fontFamily[base.slice(5)];
            if (fontFamily) return `font-family: ${fontFamily}`;
        }

        // Text alignment
        if (base === 'text-left') return 'text-align: left';
        if (base === 'text-center') return 'text-align: center';
        if (base === 'text-right') return 'text-align: right';
        if (base === 'text-justify') return 'text-align: justify';
        if (base === 'text-start') return 'text-align: start';
        if (base === 'text-end') return 'text-align: end';

        // Text decoration
        if (base === 'underline') return 'text-decoration: underline';
        if (base === 'line-through') return 'text-decoration: line-through';
        if (base === 'no-underline') return 'text-decoration: none';

        // Text transform
        if (base === 'uppercase') return 'text-transform: uppercase';
        if (base === 'lowercase') return 'text-transform: lowercase';
        if (base === 'capitalize') return 'text-transform: capitalize';
        if (base === 'normal-case') return 'text-transform: none';

        // Line height
        if (base.startsWith('leading-')) {
            const value = base.slice(8);
            if (CONFIG.lineHeight[value]) return `line-height: ${CONFIG.lineHeight[value]}`;
            const spacingValue = resolveSpacing(value);
            if (spacingValue) return `line-height: ${spacingValue}`;
        }

        // Letter spacing
        if (base.startsWith('tracking-')) {
            const value = base.slice(9);
            if (CONFIG.letterSpacing[value]) return `letter-spacing: ${CONFIG.letterSpacing[value]}`;
        }

        // Whitespace
        if (base === 'whitespace-normal') return 'white-space: normal';
        if (base === 'whitespace-nowrap') return 'white-space: nowrap';
        if (base === 'whitespace-pre') return 'white-space: pre';
        if (base === 'whitespace-pre-line') return 'white-space: pre-line';
        if (base === 'whitespace-pre-wrap') return 'white-space: pre-wrap';

        // Word break
        if (base === 'break-normal') return 'overflow-wrap: normal; word-break: normal';
        if (base === 'break-words') return 'overflow-wrap: break-word';
        if (base === 'break-all') return 'word-break: break-all';

        // List style
        if (base === 'list-none') return 'list-style-type: none';
        if (base === 'list-disc') return 'list-style-type: disc';
        if (base === 'list-decimal') return 'list-style-type: decimal';

        // Text overflow
        if (base === 'truncate') return 'overflow: hidden; text-overflow: ellipsis; white-space: nowrap';
        if (base === 'text-ellipsis') return 'text-overflow: ellipsis';
        if (base === 'text-clip') return 'text-overflow: clip';

        return null;
    }

    function resolvePosition(base) {
        const positionMap = {
            'static': 'position: static',
            'fixed': 'position: fixed',
            'absolute': 'position: absolute',
            'relative': 'position: relative',
            'sticky': 'position: sticky',
        };

        if (positionMap[base]) return positionMap[base];

        // Top, right, bottom, left
        if (base.startsWith('top-')) {
            const value = resolveSpacing(base.slice(4));
            return value ? `top: ${value}` : null;
        }
        if (base.startsWith('right-')) {
            const value = resolveSpacing(base.slice(6));
            return value ? `right: ${value}` : null;
        }
        if (base.startsWith('bottom-')) {
            const value = resolveSpacing(base.slice(7));
            return value ? `bottom: ${value}` : null;
        }
        if (base.startsWith('left-')) {
            const value = resolveSpacing(base.slice(5));
            return value ? `left: ${value}` : null;
        }

        // Inset
        if (base.startsWith('inset-')) {
            const value = resolveSpacing(base.slice(6));
            return value ? `inset: ${value}` : null;
        }

        // Z-index
        if (base.startsWith('z-')) {
            const value = base.slice(2);
            if (value === 'auto') return 'z-index: auto';
            const num = parseInt(value);
            if (!isNaN(num)) return `z-index: ${num}`;
        }

        return null;
    }

    function resolveOverflow(base) {
        const overflowMap = {
            'overflow-auto': 'overflow: auto',
            'overflow-hidden': 'overflow: hidden',
            'overflow-visible': 'overflow: visible',
            'overflow-scroll': 'overflow: scroll',
            'overflow-x-auto': 'overflow-x: auto',
            'overflow-y-auto': 'overflow-y: auto',
            'overflow-x-hidden': 'overflow-x: hidden',
            'overflow-y-hidden': 'overflow-y: hidden',
            'overflow-x-visible': 'overflow-x: visible',
            'overflow-y-visible': 'overflow-y: visible',
            'overflow-x-scroll': 'overflow-x: scroll',
            'overflow-y-scroll': 'overflow-y: scroll',
        };

        return overflowMap[base] || null;
    }

    function resolveTransforms(base) {
        if (base.startsWith('translate-x-')) {
            const value = resolveSpacing(base.slice(12));
            return value ? `transform: translateX(${value})` : null;
        }
        if (base.startsWith('translate-y-')) {
            const value = resolveSpacing(base.slice(12));
            return value ? `transform: translateY(${value})` : null;
        }
        if (base.startsWith('rotate-')) {
            const value = base.slice(7);
            const num = parseInt(value);
            if (!isNaN(num)) return `transform: rotate(${num}deg)`;
        }
        if (base.startsWith('scale-')) {
            const value = base.slice(6);
            const num = parseInt(value);
            if (!isNaN(num)) return `transform: scale(${num / 100})`;
        }
        if (base.startsWith('skew-x-')) {
            const value = base.slice(7);
            const num = parseInt(value);
            if (!isNaN(num)) return `transform: skewX(${num}deg)`;
        }
        if (base.startsWith('skew-y-')) {
            const value = base.slice(7);
            const num = parseInt(value);
            if (!isNaN(num)) return `transform: skewY(${num}deg)`;
        }
        if (base === 'transform-none') return 'transform: none';

        return null;
    }

    function resolveTransitions(base) {
        const transitionMap = {
            'transition': 'transition-property: all; transition-duration: 150ms; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1)',
            'transition-none': 'transition-property: none',
            'transition-colors': 'transition-property: color, background-color, border-color, text-decoration-color, fill, stroke; transition-duration: 150ms; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1)',
            'transition-opacity': 'transition-property: opacity; transition-duration: 150ms; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1)',
            'transition-shadow': 'transition-property: box-shadow; transition-duration: 150ms; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1)',
            'transition-transform': 'transition-property: transform; transition-duration: 150ms; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1)',
            'duration-75': 'transition-duration: 75ms',
            'duration-100': 'transition-duration: 100ms',
            'duration-150': 'transition-duration: 150ms',
            'duration-200': 'transition-duration: 200ms',
            'duration-300': 'transition-duration: 300ms',
            'duration-500': 'transition-duration: 500ms',
            'duration-700': 'transition-duration: 700ms',
            'duration-1000': 'transition-duration: 1000ms',
        };

        return transitionMap[base] || null;
    }

    function resolveInteractivity(base) {
        // Cursor
        if (base.startsWith('cursor-')) {
            const value = base.slice(7);
            if (CONFIG.cursor[value]) return `cursor: ${CONFIG.cursor[value]}`;
        }

        // Pointer events
        if (base === 'pointer-events-none') return 'pointer-events: none';
        if (base === 'pointer-events-auto') return 'pointer-events: auto';

        // User select
        if (base === 'select-none') return 'user-select: none';
        if (base === 'select-text') return 'user-select: text';
        if (base === 'select-all') return 'user-select: all';
        if (base === 'select-auto') return 'user-select: auto';

        // Resize
        if (base === 'resize-none') return 'resize: none';
        if (base === 'resize') return 'resize: both';
        if (base === 'resize-y') return 'resize: vertical';
        if (base === 'resize-x') return 'resize: horizontal';

        // Scroll behavior
        if (base === 'scroll-auto') return 'scroll-behavior: auto';
        if (base === 'scroll-smooth') return 'scroll-behavior: smooth';

        return null;
    }

    function resolveMiscellaneous(base) {
        // Visibility
        if (base === 'visible') return 'visibility: visible';
        if (base === 'invisible') return 'visibility: hidden';
        if (base === 'collapse') return 'visibility: collapse';

        // Box sizing
        if (base === 'box-border') return 'box-sizing: border-box';
        if (base === 'box-content') return 'box-sizing: content-box';

        // Aspect ratio
        if (base.startsWith('aspect-')) {
            const value = base.slice(7);
            if (value === 'auto') return 'aspect-ratio: auto';
            if (value === 'square') return 'aspect-ratio: 1 / 1';
            if (value === 'video') return 'aspect-ratio: 16 / 9';
        }

        // Object position
        if (base.startsWith('object-')) {
            const positionMap = {
                'bottom': 'bottom',
                'center': 'center',
                'left': 'left',
                'left-bottom': 'left bottom',
                'left-top': 'left top',
                'right': 'right',
                'right-bottom': 'right bottom',
                'right-top': 'right top',
                'top': 'top',
            };
            const position = positionMap[base.slice(7)];
            if (position) return `object-position: ${position}`;
        }

        // Isolation
        if (base === 'isolate') return 'isolation: isolate';
        if (base === 'isolation-auto') return 'isolation: auto';

        return null;
    }

    function resolveArbitrary(base) {
        const ARBITRARY_PREFIXES = {
            // Positioning
            'top-': 'top',
            'right-': 'right',
            'bottom-': 'bottom',
            'left-': 'left',
            'inset-': 'inset',
            'z-': 'z-index',

            // Sizing & Spacing
            'w-': 'width',
            'h-': 'height',
            'min-w-': 'min-width',
            'max-w-': 'max-width',
            'min-h-': 'min-height',
            'max-h-': 'max-height',
            'p-': 'padding',
            'pt-': 'padding-top',
            'pr-': 'padding-right',
            'pb-': 'padding-bottom',
            'pl-': 'padding-left',
            'm-': 'margin',
            'mt-': 'margin-top',
            'mr-': 'margin-right',
            'mb-': 'margin-bottom',
            'ml-': 'margin-left',
            'gap-': 'gap',

            // Layout & Flex/Grid
            'grid-cols-': 'grid-template-columns',
            'grid-rows-': 'grid-template-rows',
            'col-span-': 'grid-column',
            'row-span-': 'grid-row',

            // Colors & Styling
            'bg-': 'background-color',
            'text-': 'color',
            'border-': 'border-color',
            'rounded-': 'border-radius',
            'opacity-': 'opacity',
            'shadow-': 'box-shadow',
        };

        // Formats calc() strings to ensure valid CSS (+ and - require space padding)
        const formatValue = (val) => {
            if (val.includes('calc(')) {
                return val.replace(/calc\((.*?)\)/g, (match, expr) => {
                    const formatted = expr
                        .replace(/\s*([+-])\s*/g, ' $1 ') // Add spaces around + and -
                        .replace(/\s+/g, ' ');            // Collapse extra whitespace
                    return `calc(${formatted})`;
                });
            }
            return val;
        };

        if (base.startsWith('[') && base.endsWith(']')) {
            const content = base.slice(1, -1);
            const colonIndex = content.indexOf(':');

            if (colonIndex > 0) {
                const prop = content.slice(0, colonIndex).trim();
                const value = formatValue(content.slice(colonIndex + 1).trim().replace(/_/g, ' '));
                return `${prop}: ${value}`;
            }
        }

        const bracketStart = base.indexOf('[');
        if (bracketStart > 0 && base.endsWith(']')) {
            const prefix = base.slice(0, bracketStart);
            const rawValue = base.slice(bracketStart + 1, -1).trim().replace(/_/g, ' ');
            const value = formatValue(rawValue);

            const cssProp = ARBITRARY_PREFIXES[prefix];
            if (cssProp) {
                return `${cssProp}: ${value}`;
            }
        }
        return null;
    }

    function resolveUtility(base) {
        return (
            resolveArbitrary(base) ||
            resolveDisplay(base) ||
            resolveFlexbox(base) ||
            resolveGrid(base) ||
            resolveSpacingUtilities(base) ||
            resolveSizing(base) ||
            resolveColors(base) ||
            resolveBordersAndEffects(base) ||
            resolveTypography(base) ||
            resolvePosition(base) ||
            resolveOverflow(base) ||
            resolveTransforms(base) ||
            resolveTransitions(base) ||
            resolveInteractivity(base) ||
            resolveMiscellaneous(base)
        );
    }

    function generateRule(className) {
        // Parse variants
        const parsed = parseVariants(className);
        if (!parsed) return null;

        const declaration = resolveUtility(parsed.base);
        if (!declaration) return null;

        console.log("Declaration: ", declaration);

        let selector = '';

        const hasGroupHover = parsed.variants.some(v => v.parent);
        if (hasGroupHover) {
            selector = '.group:hover ';
        }

        console.log("class name passed is: ", className);

        selector += `.${escapeCSS(className)}`;

        parsed.variants.forEach(variant => {
            if (variant.type === 'pseudo') {
                selector += `:${variant.value}`;
            } else if (variant.type === 'dark') {
                selector = `[data-theme="dark"] ${selector}`;
            }
        });

        let rule = `${selector} { ${declaration}; }`;

        console.log("Rule: ", rule);

        const breakpoint = parsed.variants.find(v => v.type === 'breakpoint');
        if (breakpoint) {
            rule = `@media (min-width: ${CONFIG.breakpoints[breakpoint.value]}) { ${rule} }`;
        }

        return rule;
    }

    function escapeCSS(className) {
        return className.replace(/([^\w-])/g, '\\$1');
    }

    function hexToRgba(hex, opacity) {
        // Remove the hash
        hex = hex.replace('#', '');

        // Handle shorthand hex (e.g., #FFF)
        if (hex.length === 3) {
            hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
        }

        // Parse the hex values
        const r = parseInt(hex.substring(0, 2), 16);
        const g = parseInt(hex.substring(2, 4), 16);
        const b = parseInt(hex.substring(4, 6), 16);

        return `rgba(${r}, ${g}, ${b}, ${opacity})`;
    }

    function applyOpacity(color, opacityValue) {
        if (!color || !opacityValue) return color;

        // Parse opacity value (0-100)
        const opacity = parseInt(opacityValue);
        if (isNaN(opacity) || opacity < 0 || opacity > 100) return color;

        const alpha = opacity / 100;

        // Handle hex colors
        if (color.startsWith('#')) {
            return hexToRgba(color, alpha);
        }

        // Handle rgb/rgba colors
        if (color.startsWith('rgb')) {
            // If it's already rgba, replace the alpha
            if (color.startsWith('rgba')) {
                return color.replace(/[\d.]+\)$/, `${alpha})`);
            }
            // Convert rgb to rgba
            return color.replace('rgb', 'rgba').replace(')', `, ${alpha})`);
        }

        // Handle named colors or other formats
        // For simplicity, return the original color
        return color;
    }

    function initRuleStore() {
        if (state.isInitialized) return;

        // Create or retrieve dedicated style sheet
        state.styleElement = document.getElementById('tailwind-css');
        if (!state.styleElement) {
            state.styleElement = document.createElement('style');
            state.styleElement.id = 'tailwind-css';
            document.head.appendChild(state.styleElement);
        }

        state.styleSheet = state.styleElement.sheet;
        state.isInitialized = true;
    }

    function addRule(className) {
        // Check if already processed
        console.log("Processed classes: ", state.processedClasses);
        if (state.processedClasses.has(className)) return;

        // Generate rule
        const rule = generateRule(className);
        if (!rule) {
            // Mark as processed even if invalid to avoid reprocessing
            state.processedClasses.add(className);
            return;
        }

        // Add to processed classes
        state.processedClasses.add(className);

        try {
            state.styleSheet.insertRule(rule, state.styleSheet.cssRules.length);
        } catch (e) {
            console.warn(`Failed to insert rule for class "${className}":`, e);
        }
    }

    function scanNode(node) {
        if (!node || node.nodeType !== Node.ELEMENT_NODE) return;

        if (node.hasAttribute && node.hasAttribute('class')) {
            const classAttr = node.getAttribute('class');
            if (classAttr) {
                const classes = classAttr.trim().split(/\s+/);
                classes.forEach(className => {
                    if (className) addRule(className);
                });
            }
        }

        // Process children
        if (node.children) {
            Array.from(node.children).forEach(child => scanNode(child));
        }
    }

    function setupThemeObserver() {
        const observer = new MutationObserver(() => {
            const newTheme = getCurrentTheme();
            if (newTheme !== state.currentTheme) {
                state.currentTheme = newTheme;
                // Clear processed classes to regenerate with new theme colors
                state.processedClasses.clear();
                // Rescan all elements
                scanNode(document.documentElement);
            }
        });
        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['data-theme'],
        });
    }

    function setupObserver() {
        if (state.observer) return;

        state.observer = new MutationObserver((mutations) => {
            mutations.forEach(mutation => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach(node => {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            scanNode(node);
                        }
                    });
                }

                if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                    scanNode(mutation.target);
                }
            });
        });

        state.observer.observe(document.documentElement, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class'],
        });
    }

    function init() {
        state.currentTheme = getCurrentTheme();
        initRuleStore();
        scanNode(document.documentElement);
        setupObserver();
        setupThemeObserver();
    }

    // Handle DOM ready states
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();