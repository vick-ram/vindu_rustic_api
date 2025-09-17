const setupDrawers = {
    drawer: null,
    container: null,


    applyDrawerStyles: function() {
        if (this.container && this.drawer) {
            const position = this.container.dataset.position;
            const width = this.container.dataset.width;
            const height = this.container.dataset.height;

            this.drawer.style.width = '';
            this.drawer.style.height = '';

            if (position === 'left' || position === 'right') {
                this.drawer.style.width = width;
            } else if (position === 'top' || position === 'bottom') {
                this.drawer.style.height = height;
            }
        }
    },

    openDrawer: function() {
        if (this.container) {
            this.container.classList.add('active');
            document.body.style.overflow = 'hidden';

            this.drawer.style.display = '';
            this.drawer.style.visibility = '';
        }
    },

    closeDrawer: function() {
        if (this.container) {
            this.container.classList.remove('active');
            document.body.style.overflow = '';
        }
    },

    initialize: function(openBtnId, drawerId, backdropId) {
        this.container = document.getElementById(drawerId);
        if (!this.container) return;

        this.drawer = this.container.querySelector('.drawer');

        const openBtn = document.getElementById(openBtnId);
        const backdrop = document.getElementById(backdropId);
        const closeBtn = this.container.querySelector(".drawer-close-btn");

        this.applyDrawerStyles();

        // fix "this" context with bind
        openBtn.addEventListener('click', this.openDrawer.bind(this));
        closeBtn.addEventListener('click', this.closeDrawer.bind(this));
        backdrop.addEventListener('click', this.closeDrawer.bind(this));
    }
}

// Setup all drawers
setupDrawers.initialize('open-right', 'right-drawer', 'right-backdrop');


// class Drawer {
//     constructor(openBtnId, drawerId, backdropId) {
//         this.container = document.getElementById(drawerId);
//         if (!this.container) return;

//         this.drawer = this.container.querySelector('.drawer');

//         this.openBtn = document.getElementById(openBtnId);
//         this.backdrop = document.getElementById(backdropId);
//         this.closeBtn = this.container.querySelector(".drawer-close-btn");

//         this.applyDrawerStyles();
//         this.attachEventListeners();
    
//     }

//     get dataset() {
//         return this.container.dataset;
//     }

//     applyDrawerStyles() {
//         if (!this.drawer) return;

//         const { position, width, height } = this.dataset;

//         this.drawer.style.width = '';
//         this.drawer.style.height = '';


//         if (position === 'left' || position === 'right') {
//             this.drawer.style.width = width;
//         } else if (position === 'top' || position === 'bottom') {
//             this.drawer.style.height = height;
//         }
//     }

//     openDrawer = () => {
//         this.container.classList.add('active');
//         document.body.style.overflow = 'hidden';

//         this.drawer.style.display = '';
//         this.drawer.style.visibility = '';
//     };

//     closeDrawer = () => {
//         this.container.classList.remove('active');
//         document.body.style.overflow = '';
//     };

//     attachEventListeners() {
//         if (this.openBtn) this.openBtn.addEventListener('click', this.openDrawer);
//         if (this.closeBtn) this.closeBtn.addEventListener('click', this.closeDrawer);
//         if (this.backdrop) this.backdrop.addEventListener('click', this.closeDrawer);
//     }
// }


// function initDrawers(configs = []) {
//     return configs.map(cfg => new Drawer(cfg.openBtnId, cfg.drawerId, cfg.backdropId));
// }

