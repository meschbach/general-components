function enhance( Cls, name, definition ){
    var prototype = Cls.prototype;
    if( prototype[name] != undefined ){
	throw "Method '"+name+"' has already been defined on class "+Cls;
    }
    prototype[name] = definition;
}
