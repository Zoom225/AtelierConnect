import fs from 'node:fs';
import path from 'node:path';

// Refactor only complete Java identifiers. Comments, literals, SQL names and routes are untouched.
const [mode, ...pairs] = process.argv.slice(2);
if (!['--dry-run', '--write'].includes(mode) || pairs.length === 0) {
  throw new Error('Usage: node tools/rename-java-identifiers.mjs --dry-run|--write Old=New ...');
}
const replacements = new Map(pairs.map(pair => {
  const parts = pair.split('=');
  const javaIdentifier = /^[A-Za-z_$][\w$]*$/;
  const packageName = /^com\.[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)?$/;
  if (parts.length !== 2 || !(javaIdentifier.test(parts[0]) && javaIdentifier.test(parts[1])
      || packageName.test(parts[0]) && packageName.test(parts[1]))) {
    throw new Error(`Invalid Java identifier mapping: ${pair}`);
  }
  return parts;
}));
const root = process.cwd();
const sourceRoots = ['src/main/java', 'src/test/java'];

function javaFiles(directory) {
  if (!fs.existsSync(directory)) return [];
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? javaFiles(file) : entry.isFile() && file.endsWith('.java') ? [file] : [];
  });
}

function transform(source) {
  let output = '';
  let position = 0;
  let changes = 0;
  const identifierStart = /[A-Za-z_$]/;
  const identifierPart = /[A-Za-z0-9_$]/;
  while (position < source.length) {
    const remaining = source.slice(position);
    if (remaining.startsWith('//')) {
      const end = source.indexOf('\n', position);
      const stop = end < 0 ? source.length : end;
      output += source.slice(position, stop);
      position = stop;
    } else if (remaining.startsWith('/*')) {
      const end = source.indexOf('*/', position + 2);
      if (end < 0) throw new Error('Unterminated block comment');
      output += source.slice(position, end + 2);
      position = end + 2;
    } else if (remaining.startsWith('"""')) {
      const end = source.indexOf('"""', position + 3);
      if (end < 0) throw new Error('Unterminated text block');
      output += source.slice(position, end + 3);
      position = end + 3;
    } else if (source[position] === '"' || source[position] === "'") {
      const quote = source[position];
      let end = position + 1;
      while (end < source.length) {
        if (source[end] === '\\') end += 2;
        else if (source[end++] === quote) break;
      }
      output += source.slice(position, end);
      position = end;
    } else if ([...replacements.keys()].some(key => key.includes('.') && remaining.startsWith(key)
        && (position === 0 || !identifierPart.test(source[position - 1]))
        && !identifierPart.test(source[position + key.length] ?? ''))) {
      const key = [...replacements.keys()].find(candidate => candidate.includes('.') && remaining.startsWith(candidate)
        && (position === 0 || !identifierPart.test(source[position - 1]))
        && !identifierPart.test(source[position + candidate.length] ?? ''));
      output += replacements.get(key);
      position += key.length;
      changes++;
    } else if (identifierStart.test(source[position])) {
      let end = position + 1;
      while (end < source.length && identifierPart.test(source[end])) end++;
      const identifier = source.slice(position, end);
      const replacement = replacements.get(identifier);
      output += replacement ?? identifier;
      if (replacement && replacement !== identifier) changes++;
      position = end;
    } else {
      output += source[position++];
    }
  }
  return { output, changes };
}

const updates = sourceRoots.flatMap(sourceRoot => javaFiles(path.join(root, sourceRoot))).map(file => {
  const source = fs.readFileSync(file, 'utf8');
  const result = transform(source);
  const oldBase = path.basename(file, '.java');
  const newBase = replacements.get(oldBase) ?? oldBase;
  const destination = path.join(path.dirname(file), `${newBase}.java`);
  if (!path.resolve(destination).startsWith(`${path.resolve(root)}${path.sep}`)) throw new Error('Destination outside project');
  if (destination !== file && fs.existsSync(destination)) throw new Error(`Destination exists: ${destination}`);
  return { file, destination, ...result };
}).filter(update => update.changes || update.destination !== update.file);

for (const update of updates) console.log(`${path.relative(root, update.file)}: ${update.changes} identifiers${update.destination !== update.file ? ' + filename' : ''}`);
console.log(`${updates.length} Java files affected`);
if (mode === '--write') {
  for (const update of updates) {
    fs.writeFileSync(update.file, update.output, 'utf8');
    if (update.destination !== update.file) fs.renameSync(update.file, update.destination);
  }
}
