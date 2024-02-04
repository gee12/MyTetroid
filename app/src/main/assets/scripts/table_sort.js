
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

// Returns a function responsible for sorting a specific column index
// (idx = columnIndex, asc = ascending order?).
var comparer = function(colIndex, asc) {
    // This is used by the array.sort() function...
    return function(a, b) {
        // This is a transient function, that is called straight away.
        // It allows passing in different order of args, based on
        // the ascending/descending order.
        return function(v1, v2) {
            // sort based on a numeric or localeCompare, based on type...
            return (v1 !== '' && v2 !== '' && !isNaN(v1) && !isNaN(v2))
                ? v1 - v2
                : v1.toString().localeCompare(v2);
        } (getCellValue(asc ? a : b, colIndex), getCellValue(asc ? b : a, colIndex));
    }
};

function getCellValue(row, colIndex) {
    return row.children[colIndex].innerText || row.children[colIndex].textContent;
}
