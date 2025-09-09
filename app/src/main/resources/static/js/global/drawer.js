const DrawerManager = {
    currentDrawer: null,

    open: function(drawerId) {
        this.closeAll();

        const drawerContainer = document.getElementById(drawerId + '-drawer-container');
        const drawer = drawerContainer ? drawerContainer.querySelector('.drawer') : null;

        console.log("drawer exists: ", drawer)
        if (drawerContainer && drawer) {
            // make container visible
            drawerContainer.style.visibility = 'visibile';
            drawerContainer.style.display = 'block';

            // make drawer visible
            drawer.style.visibility = 'visible';
            drawer.style.display = 'block';

            // add open class to trigger animations
            drawerContainer.classList.add('drawer-open');

            // Set the opacity to backdrop whenever drawer is open
            const bacckdrop = document.querySelector('.drawer-backdrop');
            bacckdrop.style.opacity = 1;

            document.body.style.overflow = 'hidden';
            this.currentDrawer = drawerId;

            // Dispatch custom event for drawer opening
            this.dispatchDrawerEvent('drawerOpen', drawerId)
        }
    },

    close: function() {
        if (this.currentDrawer) {
            this.dispatchDrawerEvent('drawerBeforeClose', this.currentDrawer);

            const drawerContainer = document.getElementById(this.currentDrawer + '-drawer-container');
            const drawer = drawerContainer ? drawerContainer.querySelector('.drawer') : null;

            if (drawerContainer && drawer) {
                // Remove open class
                drawerContainer.classList.remove('drawer-open');

                const bacckdrop = document.querySelector('.drawer-backdrop');
                bacckdrop.style.opacity = 0;

                // wait for transition to complete before hiding
                setTimeout(() => {
                    drawerContainer.style.visibility = 'hidden';
                    drawerContainer.style.display = 'none';
                    drawer.style.visibility = 'hidden';
                    drawer.style.display = 'none';
                }, 300);
            }
        }
        document.body.style.overflow = '';

        if (this.currentDrawer) {
            this.dispatchDrawerEvent('drawerClose', this.currentDrawer);
        }

        this.currentDrawer = null;
    },

    closeAll: function() {
        this.close();
    },

    getDrawerConfig: function(drawerId) {
        const drawerContainer = document.getElementById(drawerId + '-drawer-container');

        if (!drawerContainer) return null;

        return {
            position: drawerContainer.getAttribute('data-position'),
            width: drawerContainer.getAttribute('data-width'),
            height: drawerContainer.getAttribute('data-height'),
            title: drawerContainer.getAttribute('data-title') || drawerId + ' Drawer'
        };
    },

    applyDrawerStyles: function() {
        const drawers = document.querySelectorAll('[id$="-drawer-container"]');

        drawers.forEach(container => {
            const position = container.getAttribute('data-position');
            const width = container.getAttribute('data-width');
            const height = container.getAttribute('data-height');
            const drawer = document.querySelector('.drawer');

            if (drawer) {
                drawer.style.width = '';
                drawer.style.height = '';

                if (position === 'left' || position === 'right') {
                    drawer.style.width = width;
                } else if (position === 'top' || position === 'bottom') {
                    drawer.style.height = height;
                }

                // Remove any exiting position
                drawer.classList.remove('drawer-left', 'drawer-right', 'drawer-top', 'drawer-bottom');

                // Add position class if not present
                drawer.classList.add('drawer-' + position);

                // Ensure drawer is initially hidden
                drawer.style.visibility = 'hidden';
                drawer.style.display = 'none';
            }
        });
    },

    injectContent: function() {
        const drawers = document.querySelectorAll('[id$="-drawer-container"]');

        drawers.forEach(drawerContainer => {
            const drawerId = drawerContainer.id.replace('-drawer-container', '');
            const contentSlot = drawerContainer.querySelector(`#${drawerId}-content`);
            const sourceContent = document.getElementById(drawerId + '-content');

            if (sourceContent && contentSlot && sourceContent !== contentSlot) {
                const clonedContent = sourceContent.cloneNode(true);
                clonedContent.style.display = 'block';


                contentSlot.innerHTML = '';
                contentSlot.appendChild(clonedContent);

                // Copy all classes except hidden ones
                Array.from(sourceContent.classList).forEach(className => {
                    if (!className.includes('hidden') && className !== 'display-none') {
                        contentSlot.classList.add(className);
                    }
                });

                // Copy data attributes
                Array.from(sourceContent.attributes).forEach(attr => {
                    if (attr.name.startsWith('data-')) {
                        contentSlot.setAttribute(attr.name, attr.value);
                    }
                });
            }
        });
    },

    initialize: function() {
        this.applyDrawerStyles();
        this.injectContent();
        this.setupEventListeners();
        console.log("Drawer Manager initialized")
    },

    setupEventListeners: function() {
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('drawer-backdrop')) {
                this.close();
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.currentDrawer) {
                this.close();
            }
        });


        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('drawer-close') || e.target.closest('.drawer-close')) {
                this.close();
            }
        });
    },

    dispatchDrawerEvent: function(eventName, drawerId) {
        const event = new CustomEvent(eventName, {
            detail: {
                drawerId: drawerId,
                config: this.getDrawerConfig(drawerId)
            }
        });
        document.dispatchEvent(event);
    },

    isOpen: function(drawerId) {
        return this.currentDrawer === drawerId;
    },

    toggle: function(drawerId) {
        if (this.isOpen(drawerId)) {
            this.close();
        } else {
            this.open(drawerId);
        }
    },

    debug: function() {
        const drawers = document.querySelectorAll('[id$="-drawer-container"]');
        console.log("===== Drawer Debug Info ====");

        drawers.forEach(container => {
            const drawerId = container.id.replace('-drawer-container', '');
            const drawer = container.querySelector('.drawer');
            console.log(`Drawer ${drawerId}:`, {
                visible: container.style.visibility,
                display: container.style.display,
                hasOpenClass: container.classList.contains('drawer-open'),
                drawerVisible: drawer ? drawer.style.visibility : 'no drawer found',
                drawerDisplay: drawer ? drawer.style.display : 'no drawer found',
            });
        });
    }
};

// Initialize DOM on ready
document.addEventListener('DOMContentLoaded', function() {
    DrawerManager.initialize();
});

// Global functions for html
function openDrawer(drawerId) {
    DrawerManager.open(drawerId);
    DrawerManager.debug();
}

function closeDrawer() {
    DrawerManager.close();
}

function toggleDrawer(drawerId) {
    DrawerManager.toggle(drawerId);
    DrawerManager.debug();
}

document.addEventListener('drawerOpen', function(e) {
    console.log('Drawer opened:', e.detail.drawerId, e.detail.config);
});

document.addEventListener('drawerBeforeClose', function(e) {
    console.log('Drawer about to close:', e.detail.drawerId);
});

document.addEventListener('drawerClose', function(e) {
    console.log('Drawer closed:', e.detail.drawerId);
});
