(() => {
    const POSTAL_CODE_PATTERN = /^\d{3}-?\d{4}$/;
    const addressCache = new Map();

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll('input[type="file"][data-max-files]').forEach((input) => {
            input.addEventListener("change", () => {
                const maxFiles = Number(input.dataset.maxFiles);
                if (input.files.length > maxFiles) {
                    input.value = "";
                    alert(`Images can be attached up to ${maxFiles} files.`);
                }
            });
        });

        document.querySelectorAll("form").forEach((form) => {
            form.noValidate = true;
            form.addEventListener("submit", (event) => {
                const error = validateForm(form);
                if (!error) {
                    return;
                }

                event.preventDefault();
                alert(error.message);
                error.field?.focus();
            });
        });

        document.querySelectorAll("[data-address-search]").forEach((widget) => {
            const input = widget.querySelector("[data-address-query]");
            const button = widget.querySelector("[data-address-search-button]");
            const status = widget.querySelector("[data-address-search-status]");
            const results = widget.querySelector("[data-address-search-results]");
            const form = widget.closest("form");

            if (!input || !button || !form) {
                return;
            }

            button.addEventListener("click", async () => {
                const postalCode = normalizePostalCode(input.value);
                clearResults(results);

                if (!POSTAL_CODE_PATTERN.test(postalCode)) {
                    setStatus(status, "Enter a 7-digit Japanese postal code. Example: 1000005", true);
                    return;
                }

                button.disabled = true;
                setStatus(status, "Searching.", false);

                try {
                    const addresses = await fetchPostalCode(postalCode);
                    if (addresses.length === 0) {
                        setStatus(status, "No address was found for this postal code.", true);
                        return;
                    }

                    if (addresses.length === 1) {
                        fillAddress(form, addresses[0]);
                        setStatus(status, "Address filled.", false);
                        return;
                    }

                    renderResults(results, addresses, (address) => {
                        fillAddress(form, address);
                        clearResults(results);
                        setStatus(status, "Selected address filled.", false);
                    });
                    setStatus(status, "Multiple addresses found. Select one.", false);
                } catch (error) {
                    setStatus(status, "Address search failed.", true);
                } finally {
                    button.disabled = false;
                }
            });

            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    button.click();
                }
            });
        });
    });

    function validateForm(form) {
        const fields = form.querySelectorAll("input, textarea");
        for (const field of fields) {
            if (shouldSkipField(field)) {
                continue;
            }

            const label = field.dataset.label || field.name || "Input";
            const value = field.value || "";
            if (field.required && value.trim().length === 0) {
                return { field, message: `${label} is required.` };
            }

            if (field.maxLength > -1 && value.length > field.maxLength) {
                return { field, message: `${label} must be ${field.maxLength} characters or less.` };
            }

            if (field.pattern && value.length > 0 && !matchesPattern(value, field.pattern)) {
                return { field, message: `${label} format is invalid.` };
            }

            if (field.type === "email" && value.length > 0 && !isValidEmail(value)) {
                return { field, message: `${label} format is invalid.` };
            }
        }

        const fileInput = form.querySelector('input[type="file"][data-max-files]');
        if (fileInput && fileInput.files.length > Number(fileInput.dataset.maxFiles)) {
            return {
                field: fileInput,
                message: `Images can be attached up to ${fileInput.dataset.maxFiles} files.`
            };
        }

        return null;
    }

    function shouldSkipField(field) {
        return field.disabled
            || field.type === "hidden"
            || field.type === "checkbox"
            || field.type === "submit"
            || field.type === "button"
            || field.type === "file";
    }

    function matchesPattern(value, pattern) {
        try {
            return new RegExp(`^(?:${pattern})$`).test(value);
        } catch (error) {
            return false;
        }
    }

    function isValidEmail(value) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    }

    async function fetchPostalCode(postalCode) {
        const normalizedPostalCode = normalizePostalCode(postalCode);
        if (addressCache.has(normalizedPostalCode)) {
            return addressCache.get(normalizedPostalCode);
        }

        const params = new URLSearchParams({ postalCode: normalizedPostalCode });
        const response = await fetch(`/api/japan-postal-codes?${params.toString()}`);
        if (!response.ok) {
            throw new Error("Postal code lookup failed.");
        }
        const addresses = await response.json();
        addressCache.set(normalizedPostalCode, addresses);
        return addresses;
    }

    function normalizePostalCode(value) {
        return value.trim().replace(/[^\d-]/g, "");
    }

    function renderResults(container, addresses, onSelect) {
        if (!container) {
            return;
        }

        container.replaceChildren(...addresses.map((address) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "address-result";
            button.textContent = `(${address.zipCode}) ${address.province} ${address.detailAddress}`;
            button.addEventListener("click", () => onSelect(address));
            return button;
        }));
    }

    function clearResults(container) {
        if (container) {
            container.replaceChildren();
        }
    }

    function fillAddress(form, address) {
        setAddressValue(form, "zipCode", address.zipCode);
        setAddressValue(form, "province", address.province);
        setAddressValue(form, "baseAddress", address.detailAddress);
        setFieldValue(form, "detailAddress", "");
        form.querySelector('[name="detailAddress"]')?.focus();
    }

    function setAddressValue(form, name, value) {
        const field = form.querySelector(`[data-address-field="${name}"]`);
        if (field) {
            field.value = value || "";
            field.dispatchEvent(new Event("input", { bubbles: true }));
            field.dispatchEvent(new Event("change", { bubbles: true }));
        }
    }

    function setFieldValue(form, name, value) {
        const field = form.querySelector(`[name="${name}"]`);
        if (field) {
            field.value = value;
            field.dispatchEvent(new Event("input", { bubbles: true }));
            field.dispatchEvent(new Event("change", { bubbles: true }));
        }
    }

    function setStatus(status, message, isError) {
        if (!status) {
            return;
        }
        status.textContent = message;
        status.classList.toggle("is-error", isError);
    }
})();
