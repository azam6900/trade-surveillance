import '@testing-library/jest-dom/vitest';   // adds matchers such as toHaveClass, toBeInTheDocument
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => cleanup());                    // remove what each test rendered