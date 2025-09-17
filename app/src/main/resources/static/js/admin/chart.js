
const drawCharts = {
    canvas: null,

    watchTheme: function () {
        const mutationObserver = new MutationObserver(() => {
            const theme = document.body.getAttribute("data-theme") || "light";
            this.setTheme(theme);

            // re-draw the charts here
            this.drawChartsAll();
        });
        mutationObserver.observe(document.body, {
            attributes: true,
            attributeFilter: ["data-theme"],
        });
    },

    setupDPICanvas: function (canvas) {
        const dpr = window.devicePixelRatio || 1;

        const style = getComputedStyle(canvas);
        const computedWidth = parseInt(style.width, 10);
        const computedHeight = parseInt(style.height, 10);

        // set the actual size in pixels
        canvas.width = computedWidth * dpr;
        canvas.height = computedHeight * dpr;

        // Scale the context to ensure correct drawing operations
        const ctx = canvas.getContext("2d");
        ctx.scale(dpr, dpr);

        // Set the css display size
        canvas.style.width = `${computedWidth}px`;
        canvas.style.height = `${computedHeight}px`;

        return ctx;
    },

    initialize: function (canvasId) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) {
            console.error("Canvas not found: " + canvasId);
            return null;
        }
        const context = this.setupDPICanvas(this.canvas);
        return context;
    },

    getCssVar: function (variableName) {
        const computedStyle = getComputedStyle(document.body);
        return computedStyle.getPropertyValue(variableName).trim();
    },

    currentTheme: "light",

    setTheme: function (mode) {
        if (mode === "light" || mode === "dark") {
            this.currentTheme = mode;
        } else {
            console.error("Invalid theme mode: " + mode);
        }
    },

    getStyle: function () {
        const font = "12px 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif";
        const labelColor = this.getCssVar("--color-text");
        const axisColor = this.getCssVar("--color-border");
        const primary = this.getCssVar("--color-primary");
        const success = this.getCssVar("--color-success");
        const info = this.getCssVar("--color-info");
        const warning = this.getCssVar("--color-warning");

        return {
            font,
            labelColor,
            axisColor,
            primary,
            success,
            info,
            warning,
        };
    },

    drawLine: function (ctx, data = [], options = {}) {
        if (ctx == null) return;
        const { axisColor: lineColor } = this.getStyle();

        const width = ctx.canvas.width;
        const height = ctx.canvas.height;
        ctx.clearRect(0, 0, width, height);
        const padding = options.padding || 2;
        // Calculate scaling factor
        const maxValue = Math.max(...data);
        const minValue = Math.min(...data);
        const valueRange = maxValue - minValue || 1;
        const scaleY = (height - padding * 2) / valueRange;
        const pointWidth = (width - padding * 2) / (data.length - 1);

        const fillColor = options.fillColor || "rgba(72, 187, 120, 0.3)";
        const strokeColor = options.strokeColor || "#48bb78";
        const lineWidth = options.lineWidth || 2;

        let progress = 0;
        const animate = () => {
            ctx.clearRect(0, 0, width, height);
            ctx.beginPath();
            ctx.strokeStyle = options.color || lineColor;
            ctx.lineWidth = lineWidth;

            const visiblePoints = Math.floor(progress * (data.length - 1));

            data.forEach((val, i) => {
                if (i > visiblePoints) return;
                const x = padding + (i / (data.length - 1)) * (width - padding * 2);
                const y = height - padding - (val - minValue) * scaleY;
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            });

            ctx.stroke();

            if (visiblePoints > 0) {
                ctx.lineTo(padding + visiblePoints * pointWidth, height - padding);
                ctx.lineTo(padding, height - padding);
                ctx.closePath();

                const gradient = ctx.createLinearGradient(0, 0, 0, height);
                gradient.addColorStop(0, fillColor);
                gradient.addColorStop(1, "rgba(72, 187, 120, 0.0)");

                ctx.fillStyle = gradient;
                ctx.fill();
            }

            // Draw points
            if (options.showPoints) {
                for (let i = 0; i <= visiblePoints; i++) {
                    if (options.pointStep && i % options.pointStep !== 0) continue;
                    const x = padding + i * pointWidth;
                    const y = height - padding - (data[i] - minValue) * scaleY;
                    ctx.beginPath();
                    ctx.arc(x, y, options.pointRadius || 3, 0, Math.PI * 2);
                    ctx.fillStyle = options.pointColor || "#ffffff";
                    ctx.fill();
                    ctx.strokeStyle = strokeColor;
                    ctx.lineWidth = 1;
                    ctx.stroke();
                }
            }

            if (progress < 1) {
                progress += options.speed || 0.02;
                requestAnimationFrame(animate);
            }
        };
        animate();
    },
    drawHalfDonut: function (ctx, value, maxValue, options = {}) {
        if (!ctx) return;
        const { font, labelColor, primary } = this.getStyle();

        const width = ctx.canvas.width;
        const height = ctx.canvas.height;

        ctx.clearRect(0, 0, width, height);

        const centerX = width / 2;
        const centerY = height - 10;
        const radius = Math.min(centerX, centerY) - 15;
        const lineWidth = options.lineWidth || Math.max(12, radius * 0.15);

        ctx.lineWidth = lineWidth;
        ctx.lineCap = "round";
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = "high";

        // Draw background once
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, Math.PI, 0, false);
        ctx.strokeStyle = options.bgColor || "#e2e8f0";
        ctx.stroke();

        let progress = 0;
        const target = (value / maxValue) * Math.PI;
        const animationDuration = 1000;
        let startTime = null;

        const animate = (currentTime) => {
            if (!startTime) startTime = currentTime;
            const elapsed = currentTime - startTime;
            const progressRatio = Math.min(elapsed / animationDuration, 1);

            // Smooth easing function
            const easedProgress =
            progressRatio < 0.5
                ? 2 * progressRatio * progressRatio
                : -1 + (4 - 2 * progressRatio) * progressRatio;

            progress = target * easedProgress;

            // Clear only the arc area (optimization)
            const clearLeft = centerX - radius - lineWidth;
            const clearTop = centerY - radius - lineWidth;
            const clearWidth = radius * 2 + lineWidth * 2;
            const clearHeight = radius + lineWidth * 2;

            ctx.clearRect(clearLeft, clearTop, clearWidth, clearHeight);

            // Redraw Background
            ctx.beginPath();
            ctx.arc(centerX, centerY, radius, Math.PI, 0, false);
            ctx.strokeStyle = options.bgColor || "#e2e8f0";
            ctx.stroke();

            // Foreground (animated)
            ctx.beginPath();
            ctx.arc(centerX, centerY, radius, Math.PI, Math.PI + progress, false);
            ctx.strokeStyle = options.color || primary;
            ctx.stroke();

            // Text
            if (progressRatio >= 0.99) {
                ctx.font =
                options.font ||
                `${Math.max(
                  16,
                  radius * 0.22
                )}px 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif`;
                ctx.fillStyle = options.textColor || labelColor;
                ctx.textAlign = "center";
                ctx.textBaseline = "middle";
                const percent = Math.round((value / maxValue) * 100);

                const textY = centerY - radius / 2 + 10;
                ctx.fillText(`${percent}%`, centerX, textY);
            }

            if (progressRatio < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    },

    drawDonut: function (ctx, data = [], options = {}) {
        if (!ctx || !data.length) return;
        const { font, labelColor, primary } = this.getStyle ? this.getStyle() : {};

        const width = ctx.canvas.width;
        const height = ctx.canvas.height;

        ctx.clearRect(0, 0, width, height);

        const centerX = width / 2;
        const centerY = height / 2;
        const radius = Math.min(centerX, centerY) - 10;
        const lineWidth = options.lineWidth || Math.max(8, radius * 0.35);

        ctx.lineWidth = lineWidth;
        ctx.lineCap = "butt";

        const total = data.reduce((a, b) => a + b.value, 0);

        let arcs = []; // store arc geometry for tooltips

        let startAngle = -Math.PI / 2;
        let progress = 0;
        const animationDuration = options.duration || 1200;
        let startTime = null;

        const animate = (currentTime) => {
            if (!startTime) startTime = currentTime;
            const elapsed = currentTime - startTime;
            const progressRatio = Math.min(elapsed / animationDuration, 1);

            // easing
            const eased =
            progressRatio < 0.5
                ? 2 * progressRatio * progressRatio
                : -1 + (4 - 2 * progressRatio) * progressRatio;

            progress = eased;

            ctx.clearRect(0, 0, width, height);

            // Background ring
            ctx.beginPath();
            ctx.arc(centerX, centerY, radius, 0, Math.PI * 2, false);
            ctx.strokeStyle = options.bgColor || "#e2e8f0";
            ctx.stroke();

            arcs = [];
            let currentStart = startAngle;

            data.forEach((d, i) => {
                const sliceAngle = (d.value / total) * Math.PI * 2 * progress;
                const endAngle = currentStart + sliceAngle;

                ctx.beginPath();
                ctx.arc(centerX, centerY, radius, currentStart, endAngle);
                ctx.strokeStyle =
                d.color || options.colors?.[i] || primary || "#3182ce";
                ctx.stroke();

                arcs.push({ start: currentStart, end: endAngle, data: d });

                currentStart = endAngle;
            });

            if (progressRatio >= 1 && options.showCenterLabel) {
                ctx.font = options.font || font || "16px Arial";
                ctx.fillStyle = options.textColor || labelColor || "#2d3748";
                ctx.textAlign = "center";
                ctx.textBaseline = "middle";
                ctx.fillText(options.centerText || "Donut", centerX, centerY);
            }

            if (progressRatio < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);

        // --- Tooltip handling ---
        const canvas = ctx.canvas;
        const tooltip = document.createElement("div");
        tooltip.style.position = "absolute";
        tooltip.style.padding = "4px 8px";
        tooltip.style.background = "#2d3748";
        tooltip.style.color = "white";
        tooltip.style.borderRadius = "4px";
        tooltip.style.fontSize = "12px";
        tooltip.style.pointerEvents = "none";
        tooltip.style.opacity = 0;
        document.body.appendChild(tooltip);

        canvas.addEventListener("mousemove", (e) => {
            const rect = canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            const mouseY = e.clientY - rect.top;

            const dx = mouseX - centerX;
            const dy = mouseY - centerY;
            const distance = Math.sqrt(dx * dx + dy * dy);

            if (
            distance < radius + lineWidth / 2 &&
            distance > radius - lineWidth / 2
            ) {
                let angle = Math.atan2(dy, dx);
                if (angle < -Math.PI / 2) angle += Math.PI * 2; // normalize

                const hit = arcs.find((a) => angle >= a.start && angle <= a.end);
                if (hit) {
                    tooltip.innerHTML = `${hit.data.label || "Value"}: ${hit.data.value}`;
                    tooltip.style.left = e.pageX + 10 + "px";
                    tooltip.style.top = e.pageY + 10 + "px";
                    tooltip.style.opacity = 1;
                    return;
                }
            }
            tooltip.style.opacity = 0;
        });

        canvas.addEventListener("mouseleave", () => {
            tooltip.style.opacity = 0;
        });
    },
    drawBar: function (ctx, data = [], labels = [], options = {}) {
        if (!ctx) return;
        const { font, labelColor, primary } = this.getStyle();

        const width = ctx.canvas.width;
        const height = ctx.canvas.height;
        ctx.clearRect(0, 0, width, height);

        const padding = options.padding || 40;
        const maxValue = data.length ? Math.max(...data) : 1;
        const scale = (height - padding * 2) / maxValue;
        const slotWidth = (width - padding * 2) / data.length;
        const barWidth = slotWidth * (options.barWidth ?? 0.15);

        let progress = 0;
        const duration = options.duration || 800;
        const start = performance.now();

        const bars = data.map((val, i) => {
            const x = padding + (i + 0.5) * slotWidth;
            const barHeight = val * scale;
            const y = height - padding - barHeight;

            return {
                x: x - barWidth / 2,
                y: y,
                width: barWidth,
                height: barHeight,
                label: labels[i],
                value: val,
            };
        });

        const animate = (timestamp) => {
            const elapsed = timestamp - start;
            progress = Math.min(elapsed / duration, 1);

            ctx.clearRect(0, 0, width, height);

            bars.forEach((bar, i) => {
                ctx.fillStyle = options.color || primary;

                // Calculate animated height based on progress
                const animatedHeight = bar.height * progress;
                const animatedY = height - padding - animatedHeight;

                // Rounded bar with animated height
                ctx.beginPath();
                ctx.roundRect(bar.x, animatedY, bar.width, animatedHeight, 5);
                ctx.fill();
            });

            // X axis labels
            labels.forEach((label, i) => {
                const x = padding + (i + 0.5) * slotWidth;
                const y = height - padding + 15;

                ctx.save(); // save the current state
                ctx.translate(x, y); // move to label position

                // On mobile (or small width), rotate -45deg
                if (window.innerWidth < 640 || width < 400) {
                    ctx.rotate(-Math.PI / 4); // -45 degress
                    ctx.textAlign = "right";
                } else {
                    ctx.textAlign = "center";
                }


                ctx.font = font;
                ctx.fillStyle = labelColor;
                ctx.textAlign = "center";
                ctx.fillText(label, 0, 0);
                ctx.restore();
            });

            // Y axis labels
            if (options.showYLabel) {
                const tickCount = 5;
                for (let i = 0; i <= tickCount; i++) {
                    const value = Math.round((maxValue / tickCount) * i);
                    const y = height - padding - value * scale;

                    ctx.font = font;
                    ctx.fillStyle = labelColor;
                    ctx.textAlign = "right";
                    ctx.fillText(value, padding - 5, y + 3);
                }
            }

            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);

        // --- Tooltip handling ---
        const canvas = ctx.canvas;
        // Create tooltip only once (check if it already exists)
        let tooltip = document.getElementById("chart-tooltip");
        if (!tooltip) {
            tooltip = document.createElement("div");
            tooltip.id = "chart-tooltip";
            tooltip.style.position = "absolute";
            tooltip.style.padding = "4px 8px";
            tooltip.style.background = "#2d3748";
            tooltip.style.color = "white";
            tooltip.style.borderRadius = "4px";
            tooltip.style.fontSize = "12px";
            tooltip.style.pointerEvents = "none";
            tooltip.style.opacity = 0;
            tooltip.style.transition = "opacity 0.3s ease";
            tooltip.style.zIndex = "1000";
            document.body.appendChild(tooltip);
        }

        // Remove existing event listeners to prevent duplicates
        canvas.removeEventListener("mousemove", this.tooltipMoveHandler);
        canvas.removeEventListener("mouseleave", this.tooltipLeaveHandler);

        // Create new handlers
        this.tooltipMoveHandler = (e) => {
            const rect = canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            const mouseY = e.clientY - rect.top;

            let found = false;
            for (const bar of bars) {
                if (
                mouseX >= bar.x &&
                mouseX <= bar.x + bar.width &&
                mouseY >= bar.y &&
                mouseY <= bar.y + bar.height
                ) {
                    tooltip.innerText = `${bar.label}: ${bar.value}`;
                    tooltip.style.left = `${e.clientX + 10}px`;
                    tooltip.style.top = `${e.clientY + 10}px`;
                    tooltip.style.opacity = 1;
                    found = true;
                    break;
                }
            }

            if (!found) tooltip.style.opacity = 0;
        };

        this.tooltipLeaveHandler = () => {
            tooltip.style.opacity = 0;
        };

        // Add event listeners
        canvas.addEventListener("mousemove", this.tooltipMoveHandler);
        canvas.addEventListener("mouseleave", this.tooltipLeaveHandler);
    },
    drawChartsAll: function () {
        const barCtx = drawCharts.initialize("barChart");
        const earningBarCtx = drawCharts.initialize("weekly-earnings-barChart");
        const reviewCtx = drawCharts.initialize("review-barchart");
        const halfDonutCtx = drawCharts.initialize("donut");
        const lineCtx = drawCharts.initialize("line-chart");
        const donutCtx = drawCharts.initialize("leads");

        const lineData = [0, 33, 16, 66, 46, 85];
        drawCharts.drawLine(lineCtx, lineData);

        drawCharts.drawHalfDonut(halfDonutCtx, 70, 100);

        drawCharts.drawDonut(
            donutCtx,
            [
                { value: 40, color: "#48bb78", label: "Green" },
                { value: 25, color: "#4299e1", label: "Blue" },
                { value: 20, color: "#f6ad55", label: "Orange" },
                { value: 15, color: "#e53e3e", label: "Red" },
            ],
            {
                bgColor: "#edf2f7",
                duration: 1500,
                showCenterLabel: true,
                centerText: "Donut",
            }
        );

        drawCharts.drawBar(
            barCtx,
            [12, 25, 18, 30, 20, 15, 22, 28, 35, 30, 25, 18], // 12 data points
            [
                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sep",
                "Oct",
                "Nov",
                "Dec",
            ],
            { showYLabel: true }
        );

        drawCharts.drawBar(
            earningBarCtx,
            [80, 20, 65, 42, 30, 90, 56],
            ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"],
            { showYLabel: false, barWidth: 0.5 }
        );

        drawCharts.drawBar(
            reviewCtx,
            [20, 30, 40, 80, 40, 30, 20],
            ["M", "T", "W", "T", "F", "S", "S"],
            { showYLabel: false, barWidth: 0.5 }
        );
    },
};

// Initialize charts
document.addEventListener("DOMContentLoaded", function () {
    // On first render, draw the charts
    drawCharts.drawChartsAll();
    // Re-draw on theme change
    drawCharts.watchTheme();
});
