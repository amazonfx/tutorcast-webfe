var PDFManager = Class.create({
    initialize: function(board) {
        PDFJS.disableWorker = true;
        this._board = board;
//        this._canvas = this._board._backgroundCanvasTemp;
//        this._context = this._board._backgroundContextTemp;
        

        this._currentPage = this._board._currentPage;
        this._scale = 1.3;
        this._totalPages = 0;

        this._pdfMap = {};
        this._pdfDocumentMap = {};
        this._pdfPageMap = {};

        this._pdfActivePage = {};
        this._isDirty = false;

        this._requireFlush = false;
    },

    initPDF: function(self, pdfUrl, pdfPage, boardPage) {
        self._board.lockPage(self._board);
        if (boardPage != self._board._currentPage) {
            console.log("init board page is not current: returning");
            self._board.unlockPage(self._board);
            return;
        }

        var pdfOffset = pdfPage - 1;

        var boardStartPage = boardPage - pdfOffset;
        var pdfStartPage = pdfPage - pdfOffset;
        var pdfLength = pdfPage;

        for (var i = 0; i < pdfLength; i++) {
            self.addPDFObj(self, pdfUrl, pdfStartPage + i, boardStartPage + i);
        }
        self._board.unlockPage(self._board);
    },

    setDirty: function(self) {
        self._isDirty = true;
    },
    
    updatePDFState: function(self, pdfState, deferred) {
        self._pdfMap = pdfState;
        self._isDirty = true;
        if (!deferred) {
            self.finalizePDFState(self, false);
        }
    },

    finalizePDFState: function(self, force) {
        if (!self._isDirty) {
            return;
        } else {
            self._isDirty = false;
        }

        var correctState = self._pdfMap[self._board._currentPage];
        if (correctState == undefined || correctState == null) {
            return;
        }

        if (force || correctState.url != self._pdfActivePage.url || correctState.page != self._pdfActivePage.page) {
            self.renderPage(self, self._board._currentPage, false);
        }
    },

    getPDFDocument: function(self, url) {
        var deferred = $j.Deferred();

        var pdfDoc = self._pdfDocumentMap[url];
        if (!pdfDoc != undefined && pdfDoc != null) {
            deferred.resolve(pdfDoc);
        } else {
            PDFJS.getDocument(url).then(

            function success(_doc) {
                self._pdfDocumentMap[url] = _doc;
                deferred.resolve(_doc, url);
            },

            function fail(url) {
                deferred.reject(url);
            });
        }
        return deferred;
    },

    getPDFPage: function(self, doc, url, page) {
        var deferred = $j.Deferred();

        var pdfPage = self._pdfPageMap[url + "|" + page];
        if (!pdfPage != undefined && pdfPage != null) {
            deferred.resolve(pdfPage);
        } else {
            doc.getPage(page).then(

            function sucess(loadedpage) {
            	//do not cache pages for now too memory intensive
                self._pdfPageMap[url + "|" + page] = loadedpage;
                deferred.resolve(loadedpage);
            }, function fail() {
                deferred.reject();
            });
        }
        return deferred;
    },

    addPDFObj: function(self, pdfUrl, pdfPage, boardPage) {
        var pdf = {
            url: pdfUrl,
            page: pdfPage
        };
        self._pdfMap[boardPage] = pdf;
        self._requireFlush = true;
    },

    getPDF: function(self, boardPage) {
        var pdf = self._pdfMap[boardPage];
        return self._pdfMap[boardPage];
    },

    pageExists: function(self, boardPage) {
        var pdf = self._pdfMap[boardPage];
        if (pdf == undefined || pdf == null) {
            return false;
        } else {
            return true;
        }
    },

    setActivePage: function(self, pdfUrl, page) {
        self._pdfActivePage = {
            url: pdfUrl,
            page: page
        };
    },

    getActivePage: function(self, pdfUrl) {
        return self._pdfActivePage;
    },

    renderPage: function(self, boardPage, broadcast) {
        self._board.lockPage(self._board);
        var pdf = self.getPDF(self, boardPage);

        if (pdf == undefined || pdf == null) {
            console.log("no pdf defined for page: returning");
            self._board.unlockPage(self._board);
            return;
        }

        if (boardPage != self._board._currentPage) {
            console.log("requested board page is not current: returning");
            self._board.unlockPage(self._board);
            return;
        }

        var url = pdf.url;
        var desiredPage = pdf.page;

        self.setActivePage(self, url, desiredPage);

        var tempCanvas = document.createElement("canvas");
        tempCanvas.width = self._board._backgroundCanvas.width;
        tempCanvas.height = self._board._backgroundCanvas.height;
        var tempContext = tempCanvas.getContext("2d");
        var intendedPage = boardPage;
        
        var loadDocPromise = $j.when(self.getPDFDocument(self, url));
        loadDocPromise.done(function sucess(_pdfDoc) {
            if (desiredPage > _pdfDoc.numPage || desiredPage < 1) {
                console.log("pdf page out of range, returning");
                self._board.unlockPage(self._board);
                return;
            }
            var loadPagePromise = $j.when(self.getPDFPage(self, _pdfDoc, url, desiredPage));
            loadPagePromise.done(function success(page) {
                var viewport = page.getViewport(self._scale);
                viewport.height = tempCanvas.height;
                viewport.width = tempCanvas.width;
                var renderContext = {
                    canvasContext: tempContext,
                    viewport: viewport
                };

                var totalBoardPage = boardPage + _pdfDoc.numPages - desiredPage;
                if (totalBoardPage > self._board._totalPages) {
                    self._board.updatePageDisplay(self._board, self._board._currentPage, totalBoardPage);
                }


                if (boardPage == self._board._currentPage) {
                    page.render(renderContext).then(function success() {
                        self._currentPage = desiredPage;
                        self._totalPages = _pdfDoc.numPages;
                        if (desiredPage == 1) {
                            var currentBoardPage = boardPage;
                            for (var i = 1; i <= _pdfDoc.numPages; i++) {
                                var pdfPage = i;
                                self.addPDFObj(self, url, pdfPage, currentBoardPage);
                                ++currentBoardPage;
                            }
                        }
                        if (self._board._currentPage == intendedPage){
                        	self.renderPDFToActual(self, tempCanvas);
                        }
 
                        self._board.unlockPage(self._board);
                        if (broadcast) {
                            self.sendCommand(self, self.getPDFStateMessage(self));
                        }
                    })
                }
            });
        });
        loadDocPromise.fail(function() {
            toastr.error("failed to load pdf document please refresh");
        });
    },

    generateReplayCommand: function(self, pdf, currentBoardPage) {
        var prefix = "D";

        var msg = prefix + "|" + currentBoardPage + "|" + pdf.page + "|" + pdf.url;
        return msg;
    },

    getPDFStateMessage: function(self) {
        var state = encode64(JSON.stringify(this._pdfMap));
        return state;
    },

    sendCommand: function(self, msg) {
        if (self._requireFlush && msg.length > 0) {
            self._board._PDFBuffer.push(msg);
            self._board.broadcastPDF(self._board);
        }
    },
    
    renderPDFToActual: function(self, canvas) {
        self._board._backgroundCanvas.width = self._board._backgroundCanvas.width;
    	self._board._backgroundContext.drawImage(canvas, 0, 0);
    	canvas.width = canvas.width;
    }
});