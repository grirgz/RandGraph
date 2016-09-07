// seedGen generate a tree using a given seed
//		use randCode to generate a code
// codeGen generate a tree using a given code
//		first call codeToSeeds to  generate a seed, use a random number if ommited
//		then call seedGen to generate a tree using the given seed
//			seedGen call randCode to have a code
// randCode generate a random code given depht, network seed and value seed
//		this function have bad name
//		it is used to get a code from the given seed, seed which are generated from a code which can be random (5--) so we know the final code, not the 5-- one
//		it can also generate random seed if seed is ommited
//		
// codeToSeed convert code to seed using word_to_number 
// seedToCode convert seed to code using numberToCode for each seed

RandDelayNetwork {
	classvar <>all;
	classvar <>initialized = false;
	var <>key;
	var <>code;
	var <>tree;
	classvar <>default_grow;
	classvar <>default_simple_grow;
	classvar <>default_dict;
	classvar <>default_specs;
	classvar <>default_maker;
	classvar <>default_make_graph;
	classvar <>default_old_make_graph;
	var <>dict, <>grow;
	var <>maker;
	var <>make_graph;
	var >define_specs;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, grower, dico;
		if(all[key].isNil) {
			^super.new.init(grower, dico).prAdd(key)
		} {
			var ret = all[key];
			if(grower.notNil) {
				ret.grow = grower;
			};
			if(dico.notNil) {
				ret.dict = dico;
			};
			^ret;
		}
	}

	prAdd { arg xkey;
		key = xkey;
		all[key] = this;
	}

	init { arg grower, dico;
		this.initDefaults;
		grow = grower ? default_grow;
		dict = dico ? default_dict;
		make_graph = default_make_graph;
		maker = default_maker;
		define_specs = default_specs;
	}

	initDefaults {
		if(initialized.not) {
			default_dict = IdentityDictionary.newFrom([
				\delay, { arg si;
					DelayL.ar(si, 0.8, rrand(0.01,0.8) * [1, 1+0.01.rand] * \delay.kr) + si;
				},
				\delay2, { arg si;
					DelayL.ar(si, 0.8, rrand(0.01,0.8) * [1, 1+0.01.rand] * \delay2.kr) + si;
				},
				\flanger, { arg si;
					DelayL.ar(si, 0.8, rrand(0.01,0.8) * [1, 1+0.01.rand] * SinOsc.kr(0.5.rand).range(1,1.1) * \delay2.kr) + si;
				},
				\shift, { arg si;
					FreqShift.ar(si, 100.0.rand * [1, 1+0.01.rand] * [1,-1].choose * \shift.kr) + si / 1.2;
				},
				\shift2, { arg si;
					FreqShift.ar(si, 100.0.rand * [1, 1+0.01.rand] * [1,-1].choose * \shift2.kr) + si / 1.2;
				},
				\bpf, { arg si;
					BPF.ar(si, exprand(100,10000), 0.5.rand + 0.01) + ( si / 4 );
				},
				\brf, { arg si;
					BRF.ar(si, exprand(100,10000), 0.5.rand + 0.01) + ( si / 4 );
				},
				\dist, { arg si;
					(si * 10.rand * \distamp.kr).tanh / 2;
				},
				\end, { arg si;
					si;
				}
			]);

			default_grow = { 
				var block, algo;
				block = [
					\flanger!4,
					\delay!8,
					\delay2!8,
					\bpf!2, 
					\brf!2,
					\shift!4,
					\shift2!4,
					\dist!2,
				].flatten.choose;
				algo = [
					\par, \seq
				].wchoose([0.1,0.9].normalizeSum);
				if(algo == \par) {
					\par -> [2,4].choose.collect({
						\seq -> [ block, \end ]
					})
				} {
					\seq -> ([block] ++ (\end ! [1,2].choose))
				}
			};

			default_simple_grow = { 
				var block, algo;
				block = [
					\delay, \delay, \delay, \bpf, \shift
				].choose;
				algo = [
					\par, \seq
				].choose;
				if(algo == \par) {
					\par -> [2,4].choose.collect({
						\seq -> [ block, \end ]
					})
				} {
					\seq -> (block ! [1,4].choose) ++ [\end ]
				}
			};

			default_make_graph = { arg in, code, rdnkey;
				var sig;
				var del, shi;
				var del2, shi2;
				var distamp;
				var hasFreq, afreq;
				var sig1, sig2, sig3, sig4, sig5, sig6, sig7, sig8;
				var pitchlag;
				var pitchmix;
				var fb;
				//"default_make_graph".debug("!!!");
				rdnkey = rdnkey ? \default;
				code = code ? "4--";
				sig = in;
				fb = \fb.kr(0.01);
				del = \delay.kr(1,0.1);
				del2 = \delay2.kr(1,0.1);
				shi = \shift.kr(1);
				shi2 = \shift2.kr(1);
				distamp = \distamp.kr(1);
				pitchlag = \pitchlag.kr(1/8) / TempoClock.default.tempo;
				pitchmix = \pitchmix.kr(0.5);

				sig = LPF.ar(sig, \prelpf.kr(17000));
				sig = HPF.ar(sig, \prehpf.kr(17));

				sig = sig + (LocalIn.ar(2) * fb);

				sig = LPF.ar(sig, \lpf.kr(17000));
				sig = HPF.ar(sig, \hpf.kr(17));
				sig = RandDelayNetwork(rdnkey).ar(sig, code);

				sig1 = sig.tanh * \fbdistamp.kr(1/2.1);
				sig = SelectX.ar(\fbdistmix.kr(1), [sig, sig1]);

				sig = Limiter.ar(sig);

				sig1 = sig;
				#afreq, hasFreq = Pitch.kr(sig1).flop;
				sig1 = BRF.ar(sig1, afreq.lag(pitchlag));
				#afreq, hasFreq = Pitch.kr(sig1).flop;
				sig1 = BRF.ar(sig1, afreq.lag(pitchlag));
				sig = SelectX.ar(pitchmix, [sig, sig1]);

				LocalOut.ar(sig);
				sig = LPF.ar(sig, \postlpf.kr(17000));
				sig = HPF.ar(sig, \posthpf.kr(17));
				sig = Limiter.ar(sig);
				sig = sig * \wetamp.kr(1);
				//sig.debug("end sig");
				sig;
			};

			default_old_make_graph = { arg in, code, rdnkey;
				var sig;
				var del, shi;
				var del2, shi2;
				var distamp;
				var hasFreq, afreq;
				var sig1, sig2, sig3, sig4, sig5, sig6, sig7, sig8;
				var pitchlag = \pitchlag.kr(0.1) / ~t;
				rdnkey = rdnkey ? \default;
				code = code ? "4--";
				sig = in;
				del = \delay.kr(0.1,0.1);
				del2 = \delay2.kr(0.1,0.1);
				shi = \shift.kr(0);
				shi2 = \shift2.kr(0);
				distamp = \distamp.kr(1);

				sig = LPF.ar(sig, \prelpf.kr(17000));
				sig = HPF.ar(sig, \prehpf.kr(17));

				sig = sig + (LocalIn.ar(2) * \fb.kr(0.01));
				sig = LPF.ar(sig, \lpf.kr(17000));
				sig = HPF.ar(sig, \hpf.kr(17));
				sig = RandDelayNetwork(rdnkey).ar(sig, code);
				// cool bass : 4-1K4Q-NO5T
				sig = sig.tanh / 2.1;
				sig = Limiter.ar(sig);
				sig1 = sig;
				#afreq, hasFreq = Pitch.kr(sig1).flop;
				sig1 = BRF.ar(sig1, afreq.lag(pitchlag));
				#afreq, hasFreq = Pitch.kr(sig1).flop;
				sig1 = BRF.ar(sig1, afreq.lag(pitchlag));
				sig = SelectX.ar(\pitchmix.kr(0.5), [sig, sig1]);
				LocalOut.ar(sig);
				sig = LPF.ar(sig, \plpf.kr(17000));
				sig = HPF.ar(sig, \phpf.kr(17));
				sig = Limiter.ar(sig);
				sig = sig * \poamp.kr(1);
				//sig.debug("end sig");
				sig;
			};

			default_specs = { arg obj;
				var specs = IdentityDictionary.new;
				specs.put(\fb, ControlSpec(0.0001,0.9,\exp));
				specs.put(\lpf, \freq.asSpec);
				specs.put(\hpf, \freq.asSpec);
				specs.put(\plpf, \freq.asSpec);
				specs.put(\phpf, \freq.asSpec);
				specs.put(\postlpf, \freq.asSpec);
				specs.put(\posthpf, \freq.asSpec);
				specs.put(\prelpf, \freq.asSpec);
				specs.put(\prehpf, \freq.asSpec);
				specs.put(\delay2, \delay.asSpec);
				specs.put(\shift, ControlSpec(-5,5,\lin));
				specs.put(\shift2, ControlSpec(-5,5,\lin));
				specs.put(\pitchmix, \unipolar.asSpec);
				specs.put(\pitchlag, ControlSpec(0.001,1,\exp));

				specs.put(\fbdistamp, ControlSpec(0.01,20,\exp));
				specs.put(\fbdistmix, \unipolar.asSpec);

				specs.put(\wet10, \unipolar.asSpec);
				specs.put(\wet20, \unipolar.asSpec);
				specs.collect({ arg val, key;
					Spec.add(key, val);
					//[key, val].debug("added spec");
				});
				specs;
			};

			default_maker = { arg self, name, code, bus;
				//self.code.debug("default_maker: BEGINN?NN what is code ?");
				if(bus.notNil) {
					Ndef(name).set(\inbus, bus);
				};
				Ndef(name)[0] = { InFeedback.ar(\inbus.kr(20), 2);  };
				Ndef(name).put(10, \filter -> { arg in;
					self.make_graph.(in, code, self.key)
				});
				//self.code.debug("default_maker: what is code ?");
				Ndef(name).addHalo(\code, self.code);
				Ndef(name).put(20, \filter -> { arg in;
					// master volume
					in * \mamp.kr(1)
				});
			};
	
			/////////////////

			initialized = true;

			this.class.new(\default, default_grow, default_dict);
			this.class.new(\default).define_specs;

			// not needed because it's already by default on every instances
			//this.class.new(\default).define_specs = default_spec;
			//this.class.new(\default).maker = default_maker;
			//this.class.new(\default).make_graph = default_make_graph;
		};

	}

	define_specs { arg ... args;
		^define_specs.(*args);
	}

	seqcollect { arg in, fun;
		^if(in.isKindOf(Association)) {
			in.key -> in.value.collect(fun)
		} {
			in;
		}
	}

	growCollect { arg in;
		^this.seqcollect(in, { arg el;
			if(el == \end) {
				grow.();
			} {
				this.growCollect(el)
			}
		})
	}

	interpret { arg sig, tree, dict, envir;
		^if(tree.isKindOf(Association)) {
			switch(tree.key, 
				\par, {
					tree.value.collect({ arg el;
						this.interpret(sig, el, dict, envir)
					}).mean
				},
				\seq, {
					tree.value.do({ arg el;
						sig = this.interpret(sig, el, dict, envir)
					});
					sig;
				}
			)
		} {
			dict[tree].value(sig, envir)
		}
	}

	gentree { arg count, netseed;
		var acc, ini;
		thisThread.randSeed = netseed;
		ini = \seq -> [\end];
		acc = ini;
		count.do {
			acc = this.growCollect(acc);
		};
		^acc;
	}


	*codeToSeeds { arg word;
		var res = 0; 
		var idx = 0;
		var depth, netseed, valseed;
		var letter_to_number;
		var word_to_number;

		letter_to_number = { arg letter;
			letter.digit % 32;
		};
		word_to_number = { arg wo;
			var res = 0, idx = 0;
			wo.reverse.do({ arg letter;
				if(letter.isAlphaNum) {
					res = res + ( letter_to_number.( letter ) * (32**idx) );
					idx = idx + 1;
				}
			});
			res;
		};

		#depth, netseed, valseed = word.split($-).collect({ arg x; 
			// FIXME: prevent depth to be 10000000
			if(x.size == 0) {
				x = this.numberToCode(rrand(0,1000000));
			};
			word_to_number.(x.as(Array))
		});
		^[depth, netseed, valseed];
	}

	*numberToCode { arg num;
		var res = List.new;
		block { arg break;

			40.do { arg x;
				var div = ( num / 32 ).trunc.asInteger;
				var rest = num % 32;
				//[x, num, div, rest].debug("etape x num div rest");
				if(div < 1) {
					//debug("break");
					//[rest, rest.asInteger.asDigit, div, div.asInteger.asDigit].debug("rest then div added:");
					res.add( ( rest ).asInteger.asDigit );
					//res.add( ( div ).asInteger.asDigit );
					break.value;
				};
				res.add( ( rest ).asInteger.asDigit );
				//res.debug("res now");
				//[rest, rest.asInteger.asDigit].debug("rest added:");
				num = div;
			};
		};
		^res.reverse.join;
	}

	*seedsToCode { arg depth, netseed, valseed;
		var number_to_letters;
		^"%-%-%".format(this.numberToCode(depth), this.numberToCode(netseed), this.numberToCode(valseed));
	}

	codeGen { arg in, code;
		var depth, netseed, valseed;
		^this.seedGen(in, * this.class.codeToSeeds(code) );
	}

	*codeGen { arg in, code;
		^this.new(\default).codeGen(in, code)
	}

	*randCode { arg depth, netseed, valseed;
		var cod;
		if(depth.class == String) {
			#depth, valseed, cod = this.codeToSeeds(depth)
		};
		depth = depth ? 3;
		valseed = valseed ? rrand(1,1000000);
		netseed = netseed ? rrand(1,1000000);
		cod = this.seedsToCode(depth, netseed, valseed);
		^cod
	}

	seedGen { arg in, depth, netseed, valseed;
		code = this.class.randCode(depth, netseed, valseed);
		"RandDelayNetwork code: %".format(code).postln;

		tree = this.gentree(depth, netseed);
		thisThread.randSeed = valseed;
		^this.interpret(in, tree, dict, ());
	}

	*seedGen { arg in, depth, netseed, valseed;
		^this.new(\default).seedGen(in, depth, netseed, valseed)
	}

	*ar { arg in, depth, netseed, valseed;
		^if(depth.class == String) {
			this.codeGen(in, depth)
		} {
			this.seedGen(in, depth, netseed, valseed)
		}
	}

	ar { arg in, depth, netseed, valseed;
		^if(depth.class == String) {
			this.codeGen(in, depth)
		} {
			this.seedGen(in, depth, netseed, valseed)
		}
	}

	make { arg ... args;
		^maker.(this, *args);
	}

	getPbindCompileString { arg ndef_name=\rdn, exclude_args, begin;
		^this.class.getPbindCompileString(ndef_name, exclude_args, begin);
	}

	*getPbindCompileString { arg ndef_name=\rdn, exclude_args, begin;
		exclude_args = exclude_args ?? { [\inbus] };
		begin = begin ? "\nPbind(\n\t%\n)\n";
		^begin.format(
			Ndef(ndef_name).controlKeysValues.clump(2).collect({ arg p; 
				if(exclude_args.includes(p[0]).not) {
					"%, %,".format(p[0].asCompileString, p[1].asCompileString)
				} {
					nil
				}
			}).reject(_.isNil).join("\n\t")
		)
	}

	getPresetCompileString { arg ndef_name=\rdn, exclude_args;
		var co = Ndef(ndef_name).getHalo(\code) ? code;
		var pbind;
		pbind = this.getPbindCompileString(ndef_name, exclude_args,"Pbind(\n\t%\n).keep(1)");
		co = co.asCompileString;
		^(
			"(\nRandDelayNetwork(%).make(%, %);\n"
			++"Ndef(%).put(100, \\pset -> %);\n);\n"
		).format(key.asCompileString, ndef_name.asCompileString, co, ndef_name.asCompileString, pbind )
	}
}

// not the best place for theses useful additions
+ String {
	pbcopy {
		"xsel -ib <<EOD\n%\nEOD".format(this).unixCmd
	}

	vimpbpaste {
		"vim --servername scvim --remote-send '<Esc>\"+p<Enter>'".unixCmd;
	}

	vimpaste {
		"vim --servername scvim --remote-send '<Esc>:a<Enter>%\n<C-c>'".format(this).unixCmd;
	}
}
