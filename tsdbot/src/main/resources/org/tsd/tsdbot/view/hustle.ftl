<#-- @ftlvariable name="" type="org.tsd.tsdbot.view.HustleView" -->
<#import "layout.ftl" as layout>
<@layout.layout>
    <script src="https://code.highcharts.com/highcharts.src.js"></script>
    <h1>Maybe we should hustle as hard as we hate</h1>
    <div id="hustleChart" style="width:100%; height:400px;"></div>
    <script>
        $(function () {
            var myChart = Highcharts.chart( ${chartJson} );
        });
    </script>
</@layout.layout>