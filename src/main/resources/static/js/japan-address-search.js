(() => {
    const POSTAL_CODE_PATTERN = /^\d{3}-?\d{4}$/;
    const addressCache = new Map();

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll('input[type="file"][data-max-files]').forEach((input) => {
            input.addEventListener("change", () => {
                const maxFiles = Number(input.dataset.maxFiles);
                if (input.files.length > maxFiles) {
                    input.value = "";
                    alert(`이미지는 최대 ${maxFiles}개까지 첨부할 수 있습니다.`);
                }
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
                    setStatus(status, "일본 우편번호 7자리를 입력하세요. 예: 1000005", true);
                    return;
                }

                button.disabled = true;
                setStatus(status, "검색 중입니다.", false);

                try {
                    const addresses = await fetchPostalCode(postalCode);
                    if (addresses.length === 0) {
                        setStatus(status, "해당 우편번호의 주소가 없습니다.", true);
                        return;
                    }

                    if (addresses.length === 1) {
                        fillAddress(form, addresses[0]);
                        setStatus(status, "주소를 입력칸에 채웠습니다.", false);
                        return;
                    }

                    renderResults(results, addresses, (address) => {
                        fillAddress(form, address);
                        clearResults(results);
                        setStatus(status, "선택한 주소를 입력칸에 채웠습니다.", false);
                    });
                    setStatus(status, "여러 주소가 있습니다. 사용할 주소를 선택하세요.", false);
                } catch (error) {
                    setStatus(status, "주소 검색 중 문제가 발생했습니다.", true);
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
