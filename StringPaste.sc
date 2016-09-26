
// not the best place for theses useful additions
+ String {
	pbcopy {
		"xsel -ib <<EOD\n%\nEOD".format(this).unixCmd
	}

	vimpbpaste {
		"vim --servername scvim --remote-send '<Esc>\"+p<Enter>'".unixCmd;
	}

	vimpaste {
		"vim --servername scvim --remote-send '<Esc>:a!<Enter>%\n<C-c>'".format(this).unixCmd;
	}

	editorInsert {
		this.pbcopy;
		"vim --servername scvim --remote-send '<Esc>\"+p<Enter>'".unixCmd;
	}
}
