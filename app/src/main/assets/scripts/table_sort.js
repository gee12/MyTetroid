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
    cell.onclick = function(e) {
        e.preventDefault();
        RE.clearSelection();
        sortTableByColumn(table, cell);
    };
}

function sortTableByColumn(table, cell) {
    RE.beforeTextChange();

    const tbody = table.querySelector("tbody");
    const parent = (tbody != null) ? tbody : table;
    const rows = parent.querySelectorAll('tr:nth-child(n+2)');

    const rowsArray = toArray(rows);
    const compareFn = compareRowsFnFactory(cell.cellIndex, this.asc = !this.asc);
    rowsArray.sort(compareFn);
    for (var i = 0; i < rowsArray.length; i++) {
        parent.appendChild(rowsArray[i]);
    }

    RE.textChange();
}

function compareRowsFnFactory(colIndex, asc) {
    return function(a, b) {
        const cellValue1 = getCellValue(a, colIndex);
        const cellValue2 = getCellValue(b, colIndex);
        return asc
            ? compareCellValues(cellValue1, cellValue2)
            : compareCellValues(cellValue2, cellValue1);
    };
}

function getCellValue(row, colIndex) {
    const cell = row.children[colIndex];
    return cell.innerText || cell.textContent;
}

function compareCellValues(cellValue1, cellValue2) {
    return isEmptyOrNaN(cellValue1) || isEmptyOrNaN(cellValue2)
        ? cellValue1.toString().localeCompare(cellValue2)
        : cellValue1 - cellValue2;
}

function isEmptyOrNaN(value) {
    return value === "" || isNaN(value);
}

function toArray(nodesList) {
	var arr = [];
	for(var i = 0, node; node = nodesList[i]; ++i) {
		arr.push(node);
	}
	return arr;
}

RE.addOnAfterLoadHtmlContentEventListener(function () {
    setTablesCallbacks();
});