/* eslint-disable import/no-extraneous-dependencies */
import * as enzyme from 'enzyme';
import 'jest-enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';

enzyme.configure({ adapter: new Adapter() });
