update helm_chart
set priority = '1'
where release_name = 'my-release-name-granting-upgrade-after-scale-with-persist-scale-info-1';
update helm_chart_history
set priority = '1'
where release_name = 'my-release-name-granting-upgrade-after-scale-with-persist-scale-info-1';
