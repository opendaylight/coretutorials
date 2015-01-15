# Add capability to detect data changes to config tree in ToasterImpl

- purpose is to enable the user to grab hold of the data that has changed via restconf by writing some java code
- implement the “onDataChanged” method on the “DataChangeListener” interface in ToasterImpl.java
- dump the change into the log

