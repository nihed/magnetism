dojo.provide("dh.statistics");

dojo.require("dh.statistics.dataset");
dojo.require("dh.statistics.chart");
dojo.require("dh.statistics.selector");

function dhStatisticsInit() {
	this.selector = new dh.statistics.selector.Selector();
	document.getElementById("dhHourSelector").appendChild(this.selector.getTable());
	
	var endDate = new Date();
	var startDate = new Date(endDate.getTime() - 86400000 * 3.73);
	this.selector.setRange(startDate, endDate);

	this.graph1 = new dh.statistics.chart.Chart(600, 200);
	document.getElementById("dhGraph1").appendChild(this.graph1.getCanvas());
	this.graph1.setDataset(dh.statistics.dataset.createRandomWalk());
	
	this.graph2 = new dh.statistics.chart.Chart(600, 200);
	document.getElementById("dhGraph2").appendChild(this.graph2.getCanvas());
	this.graph2.setDataset(dh.statistics.dataset.createRandomCumulative());
}