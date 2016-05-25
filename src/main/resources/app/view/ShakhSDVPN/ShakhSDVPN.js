// js for sample app custom view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, wss, ks;

    // constants
    var dataReq = 'sampleCustomDataRequest',
        dataResp = 'sampleCustomDataResponse';

    function addKeyBindings() {
        var map = {
            space: [getData, 'Fetch data from server'],

            _helpFormat: [
                ['space']
            ]
        };

        ks.keyBindings(map);
    }

    function getData() {
        wss.sendEvent(dataReq);
    }

    function respDataCb(data) {
        $scope.data = data;
        $scope.$apply();
    }

    function hostEventCb(host) {
        $scope.hosts.append(host);
        $scope.$apply()
    }


    angular.module('ovShakhSDVPN', [])
        .controller('OvShakhSDVPNCtrl',
            ['$log', '$scope', 'WebSocketService', 'KeyService',

                function (_$log_, _$scope_, _wss_, _ks_) {
                    $log = _$log_;
                    $scope = _$scope_;
                    wss = _wss_;
                    ks = _ks_;

                    var handlers = {};
                    $scope.hosts = [];
                    $scope.data = {};

                    // data response handler
                    handlers[dataResp] = respDataCb;
                    handlers['hostEvent'] = hostEventCb;
                    wss.bindHandlers(handlers);

                    addKeyBindings();

                    // custom click handler
                    $scope.getData = getData;

                    // get data the first time...
                    getData();

                    // cleanup
                    $scope.$on('$destroy', function () {
                        wss.unbindHandlers(handlers);
                        ks.unbindKeys();
                        $log.log('OvShakhSDVPNCtrl has been destroyed');
                    });

                    $log.log('OvShakhSDVPNCtrl has been created');
                }]);

}());
