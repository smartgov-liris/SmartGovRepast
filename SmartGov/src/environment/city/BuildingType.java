package environment.city;

/**
 * Use to add a type to building when parsing osm files.
 * @author Simon Pageaud
 *
 */
public enum BuildingType {
	HOME("residential", Home.class),
	WORKOFFICE("activity", WorkOffice.class),
	NONE("none", Building.class),
	MIXED("mixed", null);

	private String name = "";
	private Class<?> classe;
	   
	BuildingType(String name){
	    this.name = name;
	}
	
	BuildingType(String name, Class<?> classe){
		this.name = name;
		this.classe = classe;
	}
	   
	public String toString(){
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public Class<?> getClasse() {
		return classe;
	}
}