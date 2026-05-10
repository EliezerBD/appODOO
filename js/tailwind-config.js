tailwind.config = {
    darkMode: "class",
    theme: {
        extend: {
            colors: {
                "on-error-container": "#ffdad6",
                "secondary-fixed-dim": "#2fd9f4",
                "tertiary-fixed-dim": "#c0c1ff",
                "on-tertiary-fixed-variant": "#2f2ebe",
                "tertiary-fixed": "#e1e0ff",
                "on-secondary-fixed": "#001f25",
                "on-secondary-container": "#00515d",
                "on-secondary": "#00363e",
                "on-primary-fixed-variant": "#004395",
                "outline": "#8c909f",
                "inverse-surface": "#e5e2e1",
                "primary-container": "#4d8eff",
                "primary": "#adc6ff",
                "on-background": "#e5e2e1",
                "surface-dim": "#131313",
                "surface-container-lowest": "#0e0e0e",
                "surface-container-highest": "#353534",
                "surface-tint": "#adc6ff",
                "primary-fixed": "#d8e2ff",
                "tertiary": "#c0c1ff",
                "error": "#ffb4ab",
                "on-primary": "#002e6a",
                "on-error": "#690005",
                "inverse-on-surface": "#313030",
                "surface-container-low": "#1c1b1b",
                "outline-variant": "#424754",
                "on-surface": "#e5e2e1",
                "error-container": "#93000a",
                "on-tertiary-fixed": "#07006c",
                "inverse-primary": "#005ac2",
                "surface-bright": "#393939",
                "on-tertiary": "#1000a9",
                "on-primary-fixed": "#001a42",
                "surface-container": "#201f1f",
                "on-secondary-fixed-variant": "#004e5a",
                "background": "#131313",
                "on-primary-container": "#00285d",
                "surface-container-high": "#2a2a2a",
                "tertiary-container": "#8083ff",
                "on-tertiary-container": "#0d0096",
                "secondary": "#5de6ff",
                "secondary-container": "#00cbe6",
                "surface": "#131313",
                "secondary-fixed": "#a2eeff",
                "on-surface-variant": "#c2c6d6",
                "surface-variant": "#353534",
                "primary-fixed-dim": "#adc6ff"
            },
            borderRadius: {
                DEFAULT: "0.25rem",
                lg: "0.5rem",
                xl: "0.75rem",
                full: "9999px"
            },
            spacing: {
                xl: "24px",
                gutter: "12px",
                lg: "16px",
                md: "12px",
                "container-margin": "16px",
                base: "6px",
                xs: "4px",
                sm: "8px",
                safe: "16px"
            },
            fontFamily: {
                "label-caps": ["Inter"],
                "body-md": ["Inter"],
                "display-lg": ["Inter"],
                "headline-md": ["Inter"],
                "title-sm": ["Inter"],
                "body-sm": ["Inter"]
            },
            fontSize: {
                "label-caps": ["10px", { lineHeight: "14px", letterSpacing: "0.05em", fontWeight: "700" }],
                "body-md": ["14px", { lineHeight: "20px", fontWeight: "400" }],
                "display-lg": ["24px", { lineHeight: "32px", letterSpacing: "-0.02em", fontWeight: "700" }],
                "headline-md": ["20px", { lineHeight: "28px", letterSpacing: "-0.01em", fontWeight: "600" }],
                "title-sm": ["16px", { lineHeight: "24px", fontWeight: "600" }],
                "body-sm": ["12px", { lineHeight: "16px", fontWeight: "400" }]
            }
        }
    }
};
