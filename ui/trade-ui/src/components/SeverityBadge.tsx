import { Badge } from 'react-bootstrap';

export default function SeverityBadge({ severity }: { severity: number }) {
    const [label, bg] = severity >= 80 ? ['High', 'danger'] : severity >= 50 ? ['Medium', 'warning'] : ['Low', 'secondary'];
    return (
        <Badge bg={bg} text={bg === 'warning' ? 'dark' : undefined}>
            {label} {severity}
        </Badge>
    );
}