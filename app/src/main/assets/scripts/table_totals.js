tableTotals_userScript();

var tableTotals_tooltipElem;

function tableTotals_userScript() {

    addStyle();

    function addStyle() {
        const style = document.createElement("style");
        style.innerHTML = '' +
            '.tableTotals_tooltip { ' +
            '    position: fixed; ' +
            '    padding: 10px 20px; ' +
            '    border: 1px solid #b3c9ce; ' +
            '    border-radius: 4px; ' +
            '    text-align: center; ' +
            '    font: italic 14px/1.3 sans-serif; ' +
            '    color: #333; ' +
            '    background: #fff; ' +
            '    box-shadow: 3px 3px 3px rgba(0, 0, 0, .3); ' +
            '    overflow-wrap: break-word; ' +
            '    word-break: break-all;  ' +
            '} ';
        document.head.appendChild(style);
    }

    function setTablesCallbacks() {
        const tables = document.querySelectorAll("table");
        for (var i = 0; i < tables.length; i++) {
            setTableCallbacks(tables[i]);
        }
    }

    function setTableCallbacks(table) {
        if (table.rows.length > 0) {
            const cells = table.rows[0].cells;
            for (var j = 0; j < cells.length; j++) {
                setCellCallback(table, cells[j]);
            }
        }
    }

    function setCellCallback(table, cell) {
        cell.oncontextmenu = function(e) {
            e.preventDefault();
            RE.clearSelection();

            const content = "Total: " + calculateTotalSum(table, cell.cellIndex);
            onShowTooltip(content, e);
        };

        cell.onmouseout = onRemoveTooltip;
    }

    function calculateTotalSum(table, colIndex) {
        var sum = 0;
        for (var i = 1; i < table.rows.length; i++) {
            const cellValue = getCellValue(table.rows[i], colIndex);
            const value = parseFloat(cellValue)
            if (isNaN(value)) {
                continue
            }
            sum += value
        }
        return sum;
    }

    function getCellValue(row, colIndex) {
        const cell = row.children[colIndex];
        return cell.innerText || cell.textContent;
    }

    function onShowTooltip(textContent, e) {
        const target = e.target;

        const tooltipElem = document.createElement("div");
        tooltipElem.className = "tableTotals_tooltip";
        tooltipElem.innerHTML = textContent;
        document.body.appendChild(tooltipElem);

        const coords = target.getBoundingClientRect();
        var left = coords.left + (target.offsetWidth - tooltipElem.offsetWidth) / 2;
        if (left < 0) left = 0;
        var top = coords.top - tooltipElem.offsetHeight - 5;
        if (top < 0) {
            top = coords.top + target.offsetHeight + 5;
        }
        var maxWidth = RE.editor.offsetWidth - 26;

        tooltipElem.style.left = left + 'px';
        tooltipElem.style.top = top + 'px';
        if (maxWidth > 0) {
            tooltipElem.style.maxWidth = maxWidth + 'px';
        }

        if (tableTotals_tooltipElem) {
            tableTotals_tooltipElem.remove();
            tableTotals_tooltipElem = null;
        }

        tableTotals_tooltipElem = tooltipElem;
    };

    function onRemoveTooltip(e) {
        if (tableTotals_tooltipElem) {
            tableTotals_tooltipElem.remove();
            tableTotals_tooltipElem = null;
        }
    };

    function removeTooltips() {
        const elements = document.getElementsByClassName("tableTotals_tooltip");
        while (elements.length > 0) {
            elements[0].parentNode.removeChild(elements[0]);
        }
    }

    RE.addOnBeforeSaveHtmlContentEventListener(function () {
        removeTooltips();
    });

    RE.addOnAfterLoadHtmlContentEventListener(function () {
        setTablesCallbacks();
    });
}