var f;
var multiple = 3;

function layout() {	
	f = new force();
	f.nodes(json);
	var links = [];
	for ( var x in json) {
		var o = json[x];
		//o.call(f.drag);
        var d = $("#"+getSimpleId(o.id));
		o.layoutPosY = d.css("top");
		//Get rid of the "px"
		o.layoutPosY = 1*o.layoutPosY.substring(0,o.layoutPosY.length-2)/multiple;
		
		o.layoutPosX = d.css("left");
		//Get rid of the "px"
		o.layoutPosX = 1*o.layoutPosX.substring(0,o.layoutPosX.length-2)/multiple;
		links = links.concat(o.adjacencies);
	}
	f.links(links);
	f.start();
	// layoutOrder();
	// layoutTimestamp();
	resize();
}

function layoutStop(){
	f.stop();
}

function functor (v) {
	return typeof v === "function" ? v : function() {
		return v;
	};
}

// A rudimentary force layout using Gauss-Seidel.

function getIndex(url) {
	for ( var x in json) {
		if (json[x].id == url)
			return x;
	}
	return null;
}

force = function() {
	var force = {}, event = dispatch("tick"), size = [ 1, 1 ], drag, alpha, friction = .9, linkDistance = d3_layout_forceLinkDistance, linkStrength = d3_layout_forceLinkStrength, charge = -30, gravity = .1, theta = .8, interval, nodes = [], links = [], distances, strengths, charges;

	function repulse(node) {
		return function(quad, x1, y1, x2, y2) {
			if (quad.point !== node) {
				var dx = quad.cx - node.layoutPosX, dy = quad.cy
						- node.layoutPosY, dn = 1 / Math
						.sqrt(dx * dx + dy * dy);
				/* Barnes-Hut criterion. */
				if ((x2 - x1) * dn < theta) {
					var k = quad.charge * dn * dn;
					node.px -= dx * k;
					node.py -= dy * k;
					return true;
				}
				if (quad.point && isFinite(dn)) {
					var k = quad.pointCharge * dn * dn;
					node.px -= dx * k;
					node.py -= dy * k;
				}
			}
			return !quad.charge;
		};
	}
	function tick() {
		if(alpha<0.05)
			return false;
		var n = nodes.length, m = links.length, q, i, // current index
		o, // current object
		s, // current source
		t, // current target
		l, // current distance
		k, // current force
		x, // x-distance
		y; // y-distance
		// gauss-seidel relaxation for links
		for (i = 0; i < m; ++i) {
			o = links[i];
			s = o.fromNode;
			t = o.toNode;
			x = t.layoutPosX - s.layoutPosX;
			y = t.layoutPosY - s.layoutPosY;
			if (l = (x * x + y * y)) {
				l = alpha * strengths[i] * ((l = Math.sqrt(l)) - distances[i])
						/ l;
				x *= l;
				y *= l;
				t.layoutPosX -= x * (k = s.weight / (t.weight + s.weight));
				t.layoutPosY -= y * k;
				s.layoutPosX += x * (k = 1 - k);
				s.layoutPosY += y * k;
			}
		}
		// apply gravity forces
		if (k = alpha * gravity) {
			x = size[0] / 2;
			y = size[1] / 2;
			i = -1;
			if (k)
				while (++i < n) {
					o = nodes[i];
					o.layoutPosX += (x - o.layoutPosX) * k;
					o.layoutPosY += (y - o.layoutPosY) * k;
				}
		}
		// compute quadtree center of mass and apply charge forces
		if (charge) {
			d3_layout_forceAccumulate(q = quadtree(nodes), alpha, charges);
			i = -1;
			while (++i < n) {
				if (!(o = nodes[i]).fixed) {
					q.visit(repulse(o));
				}
			}
		}
		// position verlet integration
		i = -1;
		while (++i < n) {
			o = nodes[i];
			if (o.fixed) {
				o.layoutPosX = o.px;
				o.layoutPosY = o.py;
			} else {
				o.layoutPosX -= (o.px - (o.px = o.layoutPosX)) * friction;
				o.layoutPosY -= (o.py - (o.py = o.layoutPosY)) * friction;
			}
		}
		event.tick({
			type : "tick",
			alpha : alpha
		});
		// simulated annealing, basically
		return (alpha *= .90) < .005;
	}
	force.nodes = function(x) {
		if (!arguments.length)
			return nodes;
		nodes = x;
		return force;
	};
	force.links = function(x) {
		if (!arguments.length)
			return links;
		links = x;
		return force;
	};
	force.size = function(x) {
		if (!arguments.length)
			return size;
		size = x;
		return force;
	};
	force.linkDistance = function(x) {
		if (!arguments.length)
			return linkDistance;
		linkDistance = functor(x);
		return force;
	};
	// For backwards-compatibility.
	force.distance = force.linkDistance;
	force.linkStrength = function(x) {
		if (!arguments.length)
			return linkStrength;
		linkStrength = functor(x);
		return force;
	};
	force.friction = function(x) {
		if (!arguments.length)
			return friction;
		friction = x;
		return force;
	};
	force.charge = function(x) {
		if (!arguments.length)
			return charge;
		charge = typeof x === "function" ? x : +x;
		return force;
	};
	force.gravity = function(x) {
		if (!arguments.length)
			return gravity;
		gravity = x;
		return force;
	};
	force.theta = function(x) {
		if (!arguments.length)
			return theta;
		theta = x;
		return force;
	};
	force.start = function() {
		var i, j, n = nodes.length, m = links.length, w = size[0], h = size[1], neighbors, o;
		for (i = 0; i < n; ++i) {
			(o = nodes[i]).index = i;
			o.weight = 0;
		}
		distances = [];
		strengths = [];
		for (i = 0; i < m; ++i) {
			o = links[i];
			o.fromNode = nodes[getIndex(o.from)];
			o.toNode = nodes[getIndex(o.to)];
			distances[i] = linkDistance.call(this, o, i);
			strengths[i] = linkStrength.call(this, o, i);
			++o.fromNode.weight;
			++o.toNode.weight;
		}
		for (i = 0; i < n; ++i) {
			o = nodes[i];
			if (isNaN(o.layoutPosX))
				o.layoutPosX = position("x", w);
			if (isNaN(o.layoutPosY))
				o.layoutPosY = position("y", h);
			if (isNaN(o.px))
				o.px = o.layoutPosX;
			if (isNaN(o.py))
				o.py = o.layoutPosY;
		}
		charges = [];
		if (typeof charge === "function") {
			for (i = 0; i < n; ++i) {
				charges[i] = +charge.call(this, nodes[i], i);
			}
		} else {
			for (i = 0; i < n; ++i) {
				charges[i] = charge;
			}
		}
		// initialize node position based on first neighbor
		function position(dimension, size) {
			var neighbors = neighbor(i), j = -1, m = neighbors.length, x;
			while (++j < m)
				if (!isNaN(x = neighbors[j][dimension]))
					return x;
			return Math.random() * size;
		}
		// initialize neighbors lazily
		function neighbor() {
			if (!neighbors) {
				neighbors = [];
				for (j = 0; j < n; ++j) {
					neighbors[j] = [];
				}
				for (j = 0; j < m; ++j) {
					var o = links[j];
					neighbors[o.fromNode.index].push(o.toNode);
					neighbors[o.toNode.index].push(o.fromNode);
				}
			}
			return neighbors[i];
		}
		return force.resume();
	};
	force.resume = function() {
		alpha = .1;
		timer(tick);
		/*for(var i = 0;i<100;i++)
			tick();
		*/
		return force;
	};
	force.stop = function() {
		alpha = 0;
		return force;
	};
	// use `node.call(force.drag)` to make nodes draggable
	force.drag = function() {
		if (!drag)
			drag = behavior.drag().origin(Object).on("dragstart", dragstart)
					.on("drag", d3_layout_forceDrag).on("dragend",
							d3_layout_forceDragEnd);
		this.on("mouseover.force", d3_layout_forceDragOver).on(
				"mouseout.force", d3_layout_forceDragOut).call(drag);
	};
	function dragstart(d) {
		d3_layout_forceDragOver(d3_layout_forceDragNode = d);
		d3_layout_forceDragForce = force;
	}
	return rebind(force, event, "on");
};
var d3_layout_forceDragForce, d3_layout_forceDragNode;
function d3_layout_forceDragOver(d) {
	d.fixed |= 2;
}
function d3_layout_forceDragOut(d) {
	if (d !== d3_layout_forceDragNode)
		d.fixed &= 1;
}
function d3_layout_forceDragEnd() {
	d3_layout_forceDrag();
	d3_layout_forceDragNode.fixed &= 1;
	d3_layout_forceDragForce = d3_layout_forceDragNode = null;
}
function d3_layout_forceDrag() {
	d3_layout_forceDragNode.px = event.layoutPosX;
	d3_layout_forceDragNode.py = event.layoutPosY;
	d3_layout_forceDragForce.resume(); // restart annealing
}
function d3_layout_forceAccumulate(quad, alpha, charges) {
	var cx = 0, cy = 0;
	quad.charge = 0;
	if (!quad.leaf) {
		var nodes = quad.nodes, n = nodes.length, i = -1, c;
		while (++i < n) {
			c = nodes[i];
			if (c == null)
				continue;
			d3_layout_forceAccumulate(c, alpha, charges);
			quad.charge += c.charge;
			cx += c.charge * c.cx;
			cy += c.charge * c.cy;
		}
	}
	if (quad.point) {
		// jitter internal nodes that are coincident
		if (!quad.leaf) {
			quad.point.layoutPosX += Math.random() - .5;
			quad.point.layoutPosY += Math.random() - .5;
		}
		var k = alpha * charges[quad.point.index];
		quad.charge += quad.pointCharge = k;
		cx += k * quad.point.layoutPosX;
		cy += k * quad.point.layoutPosY;
	}
	quad.cx = cx / quad.charge;
	quad.cy = cy / quad.charge;
}
function d3_layout_forceLinkDistance(link) {
	return 20;
}
function d3_layout_forceLinkStrength(link) {
	return 1;
}
dispatch = function() {
	var dispatch = new d3_dispatch(), i = -1, n = arguments.length;
	while (++i < n)
		dispatch[arguments[i]] = d3_dispatch_event();
	return dispatch;
};
function d3_dispatch() {
}

function d3_dispatch_event() {
	var listeners = [], listenerByName = {};
	function dispatch() {
		var z = listeners, // defensive reference
		i = -1, n = z.length, l;
		while (++i < n)
			if (l = z[i].on)
				l.apply(this, arguments);
	}
	dispatch.on = function(name, listener) {
		var l, i;
		// return the current listener, if any
		if (arguments.length < 2)
			return (l = listenerByName[name]) && l.on;
		// remove the old listener, if any (with copy-on-write)
		if (l = listenerByName[name]) {
			l.on = null;
			listeners = listeners.slice(0, i = listeners.indexOf(l)).concat(
					listeners.slice(i + 1));
			delete listenerByName[name];
		}
		// add the new listener, if any
		if (listener) {
			listeners.push(listenerByName[name] = {
				on : listener
			});
		}
		return dispatch;
	};
	return dispatch;
};
rebind = function(target, source) {
	var i = 1, n = arguments.length, method;
	while (++i < n)
		target[method = arguments[i]] = d3_rebind(target, source,
				source[method]);
	return target;
};
// Method is assumed to be a standard D3 getter-setter:
// If passed with no arguments, gets the value.
// If passed with arguments, sets the value and returns the target.
function d3_rebind(target, source, method) {
	return function() {
		var value = method.apply(source, arguments);
		return arguments.length ? target : value;
	};
}
partition = function() {
	var hierarchy = hierarchy(), size = [ 1, 1 ]; // width, height
	function position(node, x, dx, dy) {
		var children = node.children;
		node.layoutPosX = x;
		node.layoutPosY = node.depth * dy;
		node.dx = dx;
		node.dy = dy;
		if (children && (n = children.length)) {
			var i = -1, n, c, d;
			dx = node.value ? dx / node.value : 0;
			while (++i < n) {
				position(c = children[i], x, d = c.value * dx, dy);
				x += d;
			}
		}
	}
	function depth(node) {
		var children = node.children, d = 0;
		if (children && (n = children.length)) {
			var i = -1, n;
			while (++i < n)
				d = Math.max(d, depth(children[i]));
		}
		return 1 + d;
	}
	function partition(d, i) {
		var nodes = hierarchy.call(this, d, i);
		position(nodes[0], 0, size[0], size[1] / depth(nodes[0]));
		return nodes;
	}
	partition.size = function(x) {
		if (!arguments.length)
			return size;
		size = x;
		return partition;
	};
	return d3_layout_hierarchyRebind(partition, hierarchy);
};

var d3_timer_queue = null, d3_timer_interval, // is an interval (or frame)
												// active?
d3_timer_timeout; // is a timeout active?
// The timer will continue to fire until callback returns true.
timer = function(callback, delay, then) {
	var found = false, t0, t1 = d3_timer_queue;
	if (arguments.length < 3) {
		if (arguments.length < 2)
			delay = 0;
		else if (!isFinite(delay))
			return;
		then = Date.now();
	}
	// See if the callback's already in the queue.
	while (t1) {
		if (t1.callback === callback) {
			t1.then = then;
			t1.delay = delay;
			found = true;
			break;
		}
		t0 = t1;
		t1 = t1.next;
	}
	// Otherwise, add the callback to the queue.
	if (!found)
		d3_timer_queue = {
			callback : callback,
			then : then,
			delay : delay,
			next : d3_timer_queue
		};
	// Start animatin'!
	if (!d3_timer_interval) {
		d3_timer_timeout = clearTimeout(d3_timer_timeout);
		d3_timer_interval = 1;
		d3_timer_frame(d3_timer_step);
	}
}
function d3_timer_step() {
	var elapsed, now = Date.now(), t1 = d3_timer_queue;
	while (t1) {
		elapsed = now - t1.then;
		if (elapsed >= t1.delay)
			t1.flush = t1.callback(elapsed);
		t1 = t1.next;
	}
	var delay = d3_timer_flush() - now;
	if (delay > 24) {
		if (isFinite(delay)) {
			clearTimeout(d3_timer_timeout);
			d3_timer_timeout = setTimeout(d3_timer_step, delay);
		}
		d3_timer_interval = 0;
	} else {
		d3_timer_interval = 1;
		d3_timer_frame(d3_timer_step);
	}
	resize();
}
timer.flush = function() {
	var elapsed, now = Date.now(), t1 = d3_timer_queue;
	while (t1) {
		elapsed = now - t1.then;
		if (!t1.delay)
			t1.flush = t1.callback(elapsed);
		t1 = t1.next;
	}
	d3_timer_flush();
};
// Flush after callbacks, to avoid concurrent queue modification.
function d3_timer_flush() {
	var t0 = null, t1 = d3_timer_queue, then = Infinity;
	while (t1) {
		if (t1.flush) {
			t1 = t0 ? t0.next = t1.next : d3_timer_queue = t1.next;
		} else {
			then = Math.min(then, t1.then + t1.delay);
			t1 = (t0 = t1).next;
		}
	}
	return then;
}
var d3_timer_frame = window.requestAnimationFrame
		|| window.webkitRequestAnimationFrame
		|| window.mozRequestAnimationFrame || window.oRequestAnimationFrame
		|| window.msRequestAnimationFrame || function(callback) {
			setTimeout(callback, 17);
		};

// Constructs a new quadtree for the specified array of points. A
// quadtree is a
// two-dimensional recursive spatial subdivision. This implementation uses
// square partitions, dividing each square into four equally-sized squares. Each
// point exists in a unique node; if multiple points are in the same position,
// some points may be stored on internal nodes rather than leaf nodes. Quadtrees
// can be used to accelerate various spatial operations, such as the Barnes-Hut
// approximation for computing n-body forces, or collision detection.
quadtree = function(points, x1, y1, x2, y2) {
	var p, i = -1, n = points.length;
	// Type conversion for deprecated API.
	if (n && isNaN(points[0].layoutPosX))
		points = points.map(d3_geom_quadtreePoint);
	// Allow bounds to be specified explicitly.
	if (arguments.length < 5) {
		if (arguments.length === 3) {
			y2 = x2 = y1;
			y1 = x1;
		} else {
			x1 = y1 = Infinity;
			x2 = y2 = -Infinity;
			// Compute bounds.
			while (++i < n) {
				p = points[i];
				if (p.layoutPosX < x1)
					x1 = p.layoutPosX;
				if (p.layoutPosY < y1)
					y1 = p.layoutPosY;
				if (p.layoutPosX > x2)
					x2 = p.layoutPosX;
				if (p.layoutPosY > y2)
					y2 = p.layoutPosY;
			}
			// Squarify the bounds.
			var dx = x2 - x1, dy = y2 - y1;
			if (dx > dy)
				y2 = y1 + dx;
			else
				x2 = x1 + dy;
		}
	}
	// Recursively inserts the specified point p at the node n or one of its
	// descendants. The bounds are defined by [x1, x2] and [y1, y2].
	function insert(n, p, x1, y1, x2, y2) {
		if (isNaN(p.layoutPosX) || isNaN(p.layoutPosY))
			return; // ignore invalid points
		if (n.leaf) {
			var v = n.point;
			if (v) {
				// If the point at this leaf node is at the same position as the
				// new
				// point we are adding, we leave the point associated with the
				// internal node while adding the new point to a child node.
				// This
				// avoids infinite recursion.
				if ((Math.abs(v.layoutPosX - p.layoutPosX) + Math.abs(v.layoutPosY - p.layoutPosY)) < .01) {
					insertChild(n, p, x1, y1, x2, y2);
				} else {
					n.point = null;
					insertChild(n, v, x1, y1, x2, y2);
					insertChild(n, p, x1, y1, x2, y2);
				}
			} else {
				n.point = p;
			}
		} else {
			insertChild(n, p, x1, y1, x2, y2);
		}
	}
	// Recursively inserts the specified point p into a descendant of node n.
	// The
	// bounds are defined by [x1, x2] and [y1, y2].
	function insertChild(n, p, x1, y1, x2, y2) {
		// Compute the split point, and the quadrant in which to insert p.
		var sx = (x1 + x2) * .5, sy = (y1 + y2) * .5, right = p.layoutPosX >= sx, bottom = p.layoutPosY >= sy, i = (bottom << 1)
				+ right;
		// Recursively insert into the child node.
		n.leaf = false;
		n = n.nodes[i] || (n.nodes[i] = d3_geom_quadtreeNode());
		// Update the bounds as we recurse.
		if (right)
			x1 = sx;
		else
			x2 = sx;
		if (bottom)
			y1 = sy;
		else
			y2 = sy;
		insert(n, p, x1, y1, x2, y2);
	}
	// Create the root node.
	var root = d3_geom_quadtreeNode();
	root.add = function(p) {
		insert(root, p, x1, y1, x2, y2);
	};
	root.visit = function(f) {
		d3_geom_quadtreeVisit(f, root, x1, y1, x2, y2);
	};
	// Insert all points.
	points.forEach(root.add);
	return root;
};

function d3_geom_quadtreeNode() {
	return {
		leaf : true,
		nodes : [],
		point : null
	};
}
function d3_geom_quadtreeVisit(f, node, x1, y1, x2, y2) {
	if (!f(node, x1, y1, x2, y2)) {
		var sx = (x1 + x2) * .5, sy = (y1 + y2) * .5, children = node.nodes;
		if (children[0])
			d3_geom_quadtreeVisit(f, children[0], x1, y1, sx, sy);
		if (children[1])
			d3_geom_quadtreeVisit(f, children[1], sx, y1, x2, sy);
		if (children[2])
			d3_geom_quadtreeVisit(f, children[2], x1, sy, sx, y2);
		if (children[3])
			d3_geom_quadtreeVisit(f, children[3], sx, sy, x2, y2);
	}
}
function d3_geom_quadtreePoint(p) {
	return {
		x : p[0],
		y : p[1]
	};
}

function resize(){
    var minx = Infinity, maxx = -Infinity, miny = Infinity, maxy = -Infinity;
    var sumx=0, sumy=0;
    for (i in json) {
        if(!testVisible(json[i]))
        	continue;
        var x = json[i].layoutPosX;
        var y = json[i].layoutPosY;
        sumx+=x;
        sumy+=y;
        if(x > maxx) maxx = x;
        if(x < minx) minx = x;
        if(y > maxy) maxy = y;
        if(y < miny) miny = y;
    }
    //Average x and y;
    sumx/=json.length;
    sumy/=json.length;
    for(var c in json) {
    	maxx = 100; maxy = 100; minx=0;miny=0;
        var node = json[c];
        if(!testVisible(node))
        	continue;
        var d = $("#"+getSimpleId(node.id));
        if(maxy==miny)
        	;//d.css("top",(jsPlumb.offsetY+jsPlumb.height*(1)/(2)) + 'px');
        else
        	d.css("top",(jsPlumb.offsetY+jsPlumb.height/2+(multiple*(node.layoutPosY-sumy))) + 'px');
        if(maxx==minx)
        	;//d.css("left", (jsPlumb.offsetX+jsPlumb.width*(1)/(2)) + 'px');
        else
        	d.css("left", (jsPlumb.offsetX+jsPlumb.width/2+(multiple*(node.layoutPosX-sumx))) + 'px');		
    }
	jsPlumb.repaintEverything();//Everything
}
function testVisible(n){
	if(typeof n.hidden != "undefined" &&n.hidden != null && n.hidden == true)
		return false;
	//if(disabledTypes.indexOf(n.fullType)!=-1)
	//	return false;
	return true;
}