# txtresources2code
Plugin maven para generar código fuente a partir de recursos de texto que describen mensajes estáticos para usuarios.

## Instalación
```xml
<plugin>
    <groupId>io.github.fabiuxx</groupId>
    <artifactId>txtresources2code-maven-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Para poder descargar la dependencia desde Sonatype, se debe agregar la siguiente configuración al archivo `pom.xml` (o también de forma general en `settings.xml` de maven).

```xml
<repositories>
    <repository>
        <id>ossrh</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>
```

### Parámetros de Configuración
Se soportan los siguientes parámetros de configuración:

| Nombre | Descripción | Valor Por Defecto |
| --- | --- | --- |
| `outputDirectory`  | Nombre de carpeta de salida a generar. | `${project.build.directory}/generated-sources/text-messages` |
| `inputDirectory` | Nombre de carpeta desde la cual se extraeran los archivos de recursos de mensaje. | `${project.build.resources[0].directory}` |
| `packageName` | Nombre de paquete para archivo de código fuente generado. | fa.gs.resources.text |

## Licencia
Este software está distribuido bajo la licencia MIT. Ver `LICENSE.txt` para más información.