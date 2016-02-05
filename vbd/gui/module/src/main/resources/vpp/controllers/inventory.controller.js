/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

define(['app/vpp/vpp.module'], function(vpp) {

    vpp.register.controller('InventoryTableController', ['$scope', '$rootScope','$filter', 'toastService', 'VppService', '$mdDialog', 'dataService',
        function($scope, $rootScope, filter, toastService, VppService, $mdDialog, dataService) {

            $scope.getVppList = function() {
                $scope.initVppList();

                VppService.getVppList(
                    // success callback
                    function(data) {
                        $scope.vppList = data;
                        $scope.$broadcast('RELOAD_VPP_TABLE');
                    },
                    // error callback
                    function(res) {
                        console.warn("Can't load VPPs from controller. Nothing is mounted, or other error", res);
                    }
                );
            };

            $scope.initVppList = function() {
                $scope.vppList = [];
            };

            $scope.viewTopology = function(vpp) {
                $mdDialog.show({
                    controller: function() {
                        var vm = this;

                        $scope.topo = new nx.graphic.Topology({
                            height: 350,
                            width: 500,
                            scalable: true,
                            theme:'blue',
                            enableGradualScaling:true,
                            nodeConfig: {
                                color: '#414040',
                                label: 'model.label',
                                scale: 'model.scale',
                                iconType: function(vertex) {
                                    var type = vertex.get().type;
                                    if (type === 'bd') {
                                        return 'bd'
                                    } else if (type === 'vpp') {
                                        return 'switch'
                                    } else {
                                        return 'interf';
                                    }
                                }
                            },
                            linkConfig: {
                                label: 'model.label',
                                linkType: 'parallel',
                                color: function(link) {
                                    if (link.getData().type === 'tunnel') {
                                        return '#00FF00';
                                    } else {
                                        return '#414040';
                                    }
                                },
                                width: function(link) {
                                    if (link.getData().type === 'tunnel') {
                                        return 5;
                                    }
                                }
                            },
                            showIcon: true,
                            dataProcessor: 'force',
                            autoLayout: true,
                            enableSmartNode: false,
                            tooltipManagerConfig: {
                                nodeTooltipContentClass: 'TooltipNode',
                                linkTooltipContentClass: 'TooltipLink'
                            }
                        });
                        $scope.app =  new nx.ui.Application;

                        vm.vpp = vpp;
                        vm.vpp.type = 'vpp';
                        vm.vpp.label = vm.vpp.name;

                        var nodes = [].concat(vm.vpp);
                        var links = [];

                        _.forEach(vm.vpp.interfaces, function(interf, index){
                            interf.label = interf.name;
                            interf.scale = 0.5;
                            nodes.push(interf);
                            links.push({source: 0, target: index + 1});
                        });

                        console.log(vpp);
                        console.log(nodes);
                        console.log(links);

                        $scope.topo.data({
                            nodes: nodes,
                            links: links
                        });

                        this.close = function() {
                            $mdDialog.cancel();
                        };

                    },
                    onComplete: function() {
                        console.log(document.getElementById('next-vpp-topo'));
                        $scope.app.container(document.getElementById('next-vpp-topo'));
                        $scope.topo.attach($scope.app);

                    },
                    templateUrl: $scope.view_path + 'vpp-topo.html',
                    controllerAs: 'VppTopoCtrl',
                    parent: angular.element(document.body),
                    clickOutsideToClose:true
                })
            };

            $scope.addVppShowForm = function() {
                $mdDialog.show({
                    controller: function() {
                        var vm = this;
                        vm.vpp = {};
                        //function called when the cancel button ( 'x' in the top right) is clicked
                        vm.close = function() {
                            $mdDialog.cancel();
                        };

                        vm.finished = function(successful) {
                            if (successful) {
                                vm.close();
                                vm.waiting = false;
                                toastService.showToast('New VPP added!');
                                $scope.getVppList();
                            } else {
                                vm.waiting = false;
                                toastService.showToast('Error adding new VPP');
                            }
                        };

                        //function called when the update button is clicked
                        vm.updateConfig = function() {
                            vm.waiting = true;
                            VppService.mountVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finished);
                        };
                    },
                    controllerAs: 'NewVppDialogCtrl',
                    templateUrl: $scope.view_path + 'new-vpp-dialog.html',
                    parent: angular.element(document.body),
                    clickOutsideToClose:true
                })
            };

            $scope.editVppShowForm = function(vppObject) {

                $mdDialog.show({
                    controller: function() {
                        var vm = this;

                        vm.vpp = {
                            name: vppObject.name,
                            status: vppObject.status,
                            ip: vppObject.ipAddress,
                            port: vppObject.port
                        };

                        //function called when the cancel button ( 'x' in the top right) is clicked
                        vm.close = function() {
                            $mdDialog.cancel();
                        };

                        vm.finishedUpdating = function(successful) {
                            if (successful) {
                                vm.close();
                                vm.waiting = false;
                                toastService.showToast('VPP configuration updated!');
                                $scope.getVppList();
                            } else {
                                vm.waiting = false;
                                toastService.showToast('Error configuring VPP');
                            }
                        };

                        vm.finishedDeleting = function(successful) {
                            if (successful) {
                                VppService.mountVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finishedUpdating);
                                $scope.getVppList();
                            } else {
                                vm.waiting = false;
                                toastService.showToast('Error configuring VPP');
                            }
                        };

                        //function called when the update button is clicked
                        vm.updateConfig = function() {
                            //VppService.editVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finishedUpdating);
                            VppService.deleteVpp(vm.vpp, vm.finishedDeleting);
                        };
                    },
                    controllerAs: 'ConfigVppDialogCtrl',
                    templateUrl: $scope.view_path + 'config-vpp-dialog.html',
                    parent: angular.element(document.body),
                    clickOutsideToClose:true
                });
            };

            $scope.deleteVpp = function(vppObject) {

                var finished = function(successful) {
                    if (successful) {
                        toastService.showToast('Removed VPP!');
                        $scope.getVppList();
                    } else {
                        toastService.showToast('Error removing VPP');
                    }
                };

                VppService.deleteVpp(vppObject, finished);
            };

            $scope.getVppList();
        }]);

    vpp.register.controller('InventoryTableDefinitonController', ['$scope', function($scope) {

        var actionCellTemplate =
            '<md-button ng-click="grid.appScope.viewTopology(row.entity)"><span style="color:black !important;">View Topology</span></md-button>' +
            '<md-button ng-click="grid.appScope.editVppShowForm(row.entity)" ng-hide="row.entity.status === \'connected\'"><span style="color:black !important;">Edit</span></md-button>' +
            '<md-button ng-click="grid.appScope.deleteVpp(row.entity)"><span style="color:black !important;">Delete</span></md-button>';

        $scope.gridOptions = {

            expandableRowTemplate: $scope.view_path + 'inventory-table-interfaces-subgrid.tpl.html',
            //subGridVariable will be available in subGrid scope
            expandableRowScope: {
                subGridVariable: 'subGridScopeVariable'
            }

        }

        $scope.gridOptions.columnDefs = [
            { name: 'name' },
            { name: 'ipAddress'},
            { name: 'port'},
            { name: 'status'},
            { name:' ',cellTemplate: actionCellTemplate}
        ];


        //$scope.gridOptions.data = $scope.vppList;

        $scope.gridOptions.onRegisterApi = function(gridApi){
            $scope.gridApi = gridApi;
        };

        $scope.$on('RELOAD_VPP_TABLE', function(event) {
            $scope.gridOptions.data = $scope.vppList.map(function(item) {
                item.subGridOptions = {
                    columnDefs: [
                        { name:"name" },
                        { name:"phys-address"},
                        { name:"oper-status"}
                    ],
                    data: item.interfaces
                };

                return item;
            });
        });

    }]);
});