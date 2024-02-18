setTableSortCallbacks();

function setTableSortCallbacks() {
    Array.from(document.querySelectorAll("table")).forEach(table => {
        if (table.rows.length > 0) {
            var cells = table.rows[0].cells;
            Array.from(cells).forEach(cell => {
                cell.onclick = function() {
                    sortTableByColumn(table, cell);
                };
            });
        }
    });
}

function sortTableByColumn(table, cell) {
    RE.beforeTextChange();

    const tbody = table.querySelector("tbody");
    const parent = (tbody != null) ? tbody : table;

    Array.from(parent.querySelectorAll('tr:nth-child(n+2)'))
        .sort(comparer(cell.cellIndex, this.asc = !this.asc))
        .forEach(row => parent.appendChild(row) );

    RE.textChange();
}

var comparer = function(colIndex, asc) {
    return function(a, b) {
        return function(v1, v2) {
            return (v1 !== '' && v2 !== '' && !isNaN(v1) && !isNaN(v2))
                ? v1 - v2
                : v1.toString().localeCompare(v2);
        } (getCellValue(asc ? a : b, colIndex), getCellValue(asc ? b : a, colIndex));
    }
};

function getCellValue(row, colIndex) {
    var cell = row.children[colIndex];
    return cell.innerText || cell.textContent;
}