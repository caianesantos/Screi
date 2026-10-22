"""Generate the complete Markdown source delivery from the files on disk."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
files = [root / 'pom.xml', root / '.mvn/wrapper/maven-wrapper.properties']
files += sorted(path for path in (root / 'src').rglob('*') if path.is_file())
files += [Path(__file__).resolve()]
languages = {'.xml': 'xml', '.properties': 'properties', '.java': 'java',
             '.html': 'html', '.css': 'css', '.js': 'javascript', '.py': 'python'}
parts = ['# SCREI — código-fonte integral\n',
         'Cada seção contém um arquivo completo do projeto, sem omissões. '
         'Consulte `README.md` para execução, credenciais e configuração.\n']
for path in files:
    relative = path.relative_to(root).as_posix()
    language = languages.get(path.suffix, 'text')
    parts.append(f'## `{relative}`\n\n```{language}\n{path.read_text(encoding="utf-8").rstrip()}\n```\n')
(root / 'CODIGO_COMPLETO.md').write_text('\n'.join(parts), encoding='utf-8')
print(f'{len(files)} arquivos exportados integralmente para CODIGO_COMPLETO.md')
